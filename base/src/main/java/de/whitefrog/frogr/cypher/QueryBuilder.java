package de.whitefrog.frogr.cypher;

import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.Filter;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.model.relationship.Relationship;
import de.whitefrog.frogr.persistence.AnnotationDescriptor;
import de.whitefrog.frogr.persistence.FieldDescriptor;
import de.whitefrog.frogr.persistence.ModelCache;
import de.whitefrog.frogr.persistence.Persistence;
import de.whitefrog.frogr.repository.RelationshipRepository;
import de.whitefrog.frogr.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds neo4j cypher queries using SearchParameter instances.
 * Adds the required labels and builds the correct match and where clauses.
 */
public class QueryBuilder {
  private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
  
  private Repository repository;
  private final Map<String, Object> queryParams = new HashMap<>();
  private SearchParameter params;
  private final String type;
  private final Map<String, String> matches = new HashMap<>();
  // list of indexed fields
  private final List<String> queryFields = new ArrayList<>();
  private final Persistence persistence;
  private final FieldParser fieldParser;

  public QueryBuilder(Repository repository) {
    this.repository = repository;
    this.persistence = repository.service().persistence();
    this.type = persistence.cache().getModelName(repository.getModelClass());
    this.fieldParser = new FieldParser(repository);
    persistence.cache().fieldMap(repository.getModelClass()).forEach(descriptor -> {
      if(descriptor.annotations().indexed != null || descriptor.annotations().unique) {
        queryFields.add(descriptor.field().getName());
      }
    });
  }

  public Repository repository() {
    return repository;
  }

  public String id() {
    return repository().queryIdentifier();
  }

  private StringBuilder match() {
    matches.clear();
    // add required matches for ordered fields when counted
    for(SearchParameter.OrderBy order : params.orderBy()) {
      if(!order.field().contains(".") && !matches.keySet().contains(order.field())) {
        AnnotationDescriptor descriptor =
          persistence.cache().fieldAnnotations(repository().getModelClass(), order.field());
        if(descriptor.relationshipCount != null) {
          MatchBuilder match = new MatchBuilder()
            .relationship(order.field())
            .relationshipType(descriptor.relationshipCount.type());
          
          if(descriptor.relationshipCount.direction().equals(Direction.OUTGOING) ||
              descriptor.relationshipCount.direction().equals(Direction.BOTH)) {
            match.from(id()).fromLabel(type);
            if(!descriptor.relationshipCount.otherModel().equals(Model.class)) 
              match.toLabel(persistence.cache().getModelName(descriptor.relationshipCount.otherModel()));
          } else {
            match.to(id()).toLabel(type);
            if(!descriptor.relationshipCount.otherModel().equals(Model.class))
              match.fromLabel(persistence.cache().getModelName(descriptor.relationshipCount.otherModel()));
          }
          if(descriptor.relationshipCount.direction().equals(Direction.BOTH)) {
            match.undirected();
          }
          matches.put(order.field(), match.build());
        }
      }
    }

    // add required matches for filters
    for(Filter filter : params.filters()) {
      String fieldName = filter.getProperty();
      if(fieldName.contains(".")) fieldName = fieldName.substring(0, fieldName.indexOf("."));
//      if(matches.keySet().contains(getMatchName(filter.getProperty()))) continue;
      if(matches.keySet().contains(fieldName)) continue;
      
      generateFilterMatch(filter, repository().getModelClass(), id(), fieldName);
    }
    
    for(String returns: params.returns()) {
      String returnsKey = returns.contains(" ")? returns.substring(0, returns.indexOf(" ")): returns;
      if(!matches.containsKey(returnsKey)) {
        AnnotationDescriptor descriptor =
          persistence.cache().fieldAnnotations(repository().getModelClass(), returnsKey);
        if(descriptor == null) continue;
        
        if(descriptor.relatedTo != null) {
          boolean isRelationship = false;
          try {
            Field field = persistence.cache().getField(repository().getModelClass(), returnsKey);
            if(Collection.class.isAssignableFrom(field.getType())) {
              isRelationship = Relationship.class.isAssignableFrom(ReflectionUtil.getGenericClass(field));
            }
            else {
              isRelationship = Relationship.class.isAssignableFrom(field.getType());
            }
          } catch(NoSuchFieldException e) {
            logger.error(e.getMessage(), e);
          }

          MatchBuilder match = new MatchBuilder()
            .relationshipType(descriptor.relatedTo.type());
          if(isRelationship) match.relationship(returnsKey);
          if(descriptor.relatedTo.direction().equals(Direction.OUTGOING) ||
              descriptor.relatedTo.direction().equals(Direction.BOTH)) {
            match.from(id()).fromLabel(type);
            if(!isRelationship) match.to(returnsKey);
          } else {
            match.to(id()).toLabel(type);
            if(!isRelationship) match.from(returnsKey);
          }
          if(descriptor.relatedTo.direction().equals(Direction.BOTH)) {
            match.undirected();
          }
          matches.put(returnsKey, match.build());
        }
      }
    }
    
    if(matches.isEmpty()) {
      if(repository() instanceof RelationshipRepository) {
        return new StringBuilder("match ()-[" + id() + ":" + type + "]-() ");
      } else {
        return new StringBuilder("match (" + id() + ":" + type + ") ");
      }
    } else {
      return new StringBuilder("match ")
        .append(StringUtils.join(matches.values(), ", ")).append(" ");
    }
  }
  
  private void generateFilterMatch(Filter filter, Class<?> clazz, String id, String fieldName) {
    MatchBuilder match = new MatchBuilder();
    
    FieldDescriptor descriptor = persistence.cache().fieldDescriptor(clazz, fieldName);
    if(descriptor == null) {
      for(Class sub: persistence.cache().subTypesOf(clazz)) {
        descriptor = persistence.cache().fieldDescriptor(sub, fieldName);
        if(descriptor != null) break;
      }
      if(descriptor == null) {
        logger.warn("the field for filter {} could not be found", filter.getProperty());
        return;
      }
    }
    
    AnnotationDescriptor annotations = descriptor.annotations();
    String className = descriptor.baseClassName();

    if(annotations.relatedTo != null) {
      if(annotations.relatedTo.direction().equals(Direction.OUTGOING) ||
          annotations.relatedTo.direction().equals(Direction.BOTH)) {
        match.from(id);
        if(id.equals(id())) {
          match.fromLabel(type);
        }
        if(descriptor.isRelationship()) {
          match.to(fieldName + "_to"); 
        } else {
          match.to(fieldName).toLabel(className);
        }
      } else {
        match.to(id);
        if(id.equals(id())) {
          match.toLabel(type);
        }
        if(descriptor.isRelationship()) {
          match.from(fieldName + "_from");
        } else {
          match.from(fieldName).fromLabel(className);
        }
      }
      if(annotations.relatedTo.direction().equals(Direction.BOTH)) {
        match.undirected();
      }
      match.relationshipType(annotations.relatedTo.type());
      if(descriptor.isRelationship()) match.relationship(fieldName);
      
      String sub = filter.getProperty().substring(fieldName.length() + filter.getProperty().indexOf(fieldName) + 1);
      if(sub.contains(".")) {
        generateFilterMatch(filter, descriptor.baseClass(), fieldName, sub.substring(0, sub.indexOf(".")));
      }
//      matches.put(getMatchName(filter.getProperty()), newMatch.toString());
      matches.put(fieldName, match.build());
    }
  }

  private StringBuilder where() {
    List<String> wheres = new LinkedList<>();
    if(params.isFiltered()) {
      int i = 0;
      for(Filter filter : params.filters()) {
        String lookup = filter.getProperty();

        boolean lowerCaseIndex;
        if(lookup.endsWith(".id")) {
          lookup = "id(" + lookup.substring(0, lookup.length() - 3) + ")";
          lowerCaseIndex = false;
        } else {
          lowerCaseIndex = fieldParser.isLowerCase(lookup);
          if(lowerCaseIndex) {
            lookup += "_lower";
          }
          if(!lookup.contains(".")) {
            lookup = id() + "." + lookup;
          }
        }
        
        
        String marker = filter.getProperty().replaceAll("\\.", "") + i;

        Object value = filter.getValue();
        if(value != null && value instanceof Date) {
          value = ((Date) value).getTime();
        }
        // for fulltext searches we have to convert to lower case
        if(value != null && value instanceof String && lowerCaseIndex) {
          value = ((String) value).toLowerCase();
        }

        if(filter instanceof Filter.Equals) {
          if(value == null) {
            wheres.add(lookup + " IS NULL");
          }
          else {
            String where = "(";
            if(value instanceof Boolean) {
              if(value == Boolean.TRUE) {
                where+= lookup + " IS NOT NULL AND ";
              }
              else {
                where+= lookup + " IS NULL OR ";
              }
            }
            where += lookup + " = {" + marker + "})";

            wheres.add(where);
            queryParams.put(marker, value);
          }
        }
        else if(filter instanceof Filter.StartsWith) {
          wheres.add(lookup + " starts with {" + marker + "}");
          queryParams.put(marker, value);
        }
        else if(filter instanceof Filter.EndsWith) {
          wheres.add(lookup + " ends with {" + marker + "}");
          queryParams.put(marker, value);
        }
        else if(filter instanceof Filter.Contains) {
          wheres.add(lookup + " contains {" + marker + "}");
          queryParams.put(marker, value);
        }
        else if(filter instanceof Filter.NotEquals) {
          if(value == null) {
            wheres.add(lookup + " IS NOT NULL");
          }
          else {
            String where = "(" + lookup + " <> {" + marker + "}";
            if(value instanceof Boolean) {
              where += "OR " + lookup + " IS " +
                (value == Boolean.FALSE? "NOT": "") + " NULL";
            }
            wheres.add(where + ")");
            queryParams.put(marker, value);
          }
        }
        else if(filter instanceof Filter.GreaterThan) {
          String including = ((Filter.GreaterThan) filter).isIncluding()? "=": "";
          wheres.add(lookup + " >" + including + " {" + marker + "}");
          queryParams.put(marker, value);
        }
        else if(filter instanceof Filter.LessThan) {
          String including = ((Filter.LessThan) filter).isIncluding()? "=": "";
          wheres.add(lookup + " <" + including + " {" + marker + "}");
          queryParams.put(marker, value);
        }
        else if(filter instanceof Filter.Range) {
          String including = ((Filter.Range) filter).isIncluding()? "=": "";
          Filter.Range range = (Filter.Range) filter;
          wheres.add(lookup + " >" + including + " {" + marker + "_from}");
          wheres.add(lookup + " <" + including + " {" + marker + "_to}");
          queryParams.put(marker + "_from", range.getFrom());
          queryParams.put(marker + "_to", range.getTo());
        }
        i++;
      }
    }

    if(params.query() != null) {
      if(params.query().contains(":")) {
        String[] split = params.query().split(":", 2);
        String field = split[0].trim();
        String query = split[1].trim();
        if(fieldParser.isLowerCase(field)) field+= "_lower";
        if(query.isEmpty()) {
          throw new IllegalArgumentException("empty queries not allowed: \"" + params.query() + "\"");
        }
        String comparator = getQueryComparator(query);
        wheres.add(MessageFormat.format("{0}.{1} {2} '{'query'}'", id(), field, comparator));
        queryParams.put("query", query.replaceAll("\\*", ""));
      } else {
        if(params.query().isEmpty()) {
          throw new IllegalArgumentException("empty queries not allowed: \"" + params.query() + "\"");
        }
        String comparator = getQueryComparator(params.query());
        List<String> queries = new LinkedList<>();  
        for(String queryField: queryFields) {
          if(fieldParser.isLowerCase(queryField)) queryField+= "_lower";
          queries.add(MessageFormat.format("{0}.{1} {2} '{'query'}'", id(), queryField, comparator));
        }
        wheres.add("(" + StringUtils.join(queries, " OR ") + ")");
        queryParams.put("query", params.query().replaceAll("\\*", ""));
      }
    }
    if(!CollectionUtils.isEmpty(params.ids())) {
      List<String> queries = new LinkedList<>();
      int index = 1;
      for(Long id: params.ids()) {
        queries.add("id(" + id() + ") = {id_" + index + "}");
        queryParams.put("id_" + (index++), id);
      }
      wheres.add("(" + StringUtils.join(queries, " OR ") + ")");
    }
    if(!CollectionUtils.isEmpty(params.uuids())) {
      List<String> queries = new LinkedList<>();
      int index = 1;
      for(String uuid: params.uuids()) {
        queries.add(id() + ".uuid = {uuid_" + index + "}");
        queryParams.put("uuid_" + (index++), uuid);
      }
      wheres.add("(" + StringUtils.join(queries, " OR ") + ")");
    }
    
    if(!wheres.isEmpty()) {
      return new StringBuilder("where ")
        .append(StringUtils.join(wheres, " AND ")).append(" ");
    } else {
      return new StringBuilder();
    }
  }

  private String getQueryComparator(String query) {
    if(query.startsWith("*") && !query.endsWith("*")) {
      return "ends with";
    } else if(query.startsWith("*") && query.endsWith("*")) {
      return "contains";
    } else if(query.endsWith("*")) {
      return "starts with";
    } else {
      return  "=";
    }
  }

  private StringBuilder orderBy() {
    StringBuilder query = new StringBuilder();
    
    if(!params.orderBy().isEmpty()) {
      List<String> orders = new LinkedList<>();
      for(SearchParameter.OrderBy order : params.orderBy()) {
        if(!order.field().contains(".") && persistence.cache().fieldAnnotations(repository().getModelClass(), order.field()).relationshipCount != null) {
          orders.add("count(" + order.field() + ") " + order.dir());
        }
        else if(order.field().contains(".")) {
          orders.add(order.field() + " " + order.dir());
        }
        else {
          orders.add(id() + "." + order.field() + " " + order.dir());
        }
      }
      query.append(" order by ").append(StringUtils.join(orders, ", ")).append(" ");
    }
    
    return query;
  }

  private StringBuilder returns() {
    List<String> ret = new LinkedList<>();
    
    if(CollectionUtils.isEmpty(params.returns())) {
      ret.add(id());
    } else {
      final List<String> returns = new ArrayList<>(params.returns());
//      if(persistence.cache().fieldDescriptor(repository().getModelClass(), "to") == null) {
//        returns = returns.stream().map(r -> {
//          if(r.contains(".to")) return r.replace(".to", "_to");
//          else return r;
//        }).collect(Collectors.toList());
//      }
      List<String> parsed = returns.stream().map(r -> {
        if(r.contains(".")) return r.replace(".", "_");
        FieldDescriptor descriptor = persistence.cache().fieldDescriptor(repository().getModelClass(), r);
        if(!id().equals(r) && descriptor.isCollection() && returns.size() > 1) { 
          r = "collect(" + r + ") as " + r;
        }
        return r; 
      }).collect(Collectors.toList());
      ret.add(StringUtils.join(parsed, ","));
    }
    
    // to order by a relationship count we need to count it in return for cypher
    for(SearchParameter.OrderBy order : params.orderBy()) {
      if(!order.field().contains(".")) {
        AnnotationDescriptor descriptor =
          persistence.cache().fieldAnnotations(repository().getModelClass(), order.field());
        if(descriptor.relationshipCount != null) {
          ret.add("count(" + order.field() + ") as " + order.field() + "_c");
        }
      }
    }
    
    return new StringBuilder("return ").append(StringUtils.join(ret, ", ")).append(" ");
  }
  
  private StringBuilder paging() {
    StringBuilder query = new StringBuilder();
    
    if(params.page() > 1) {
      query.append("skip {skip} ");
      queryParams.put("skip", (params.page() - 1) * params.limit());
    }

    if(params.limit() < Integer.MAX_VALUE) {
      query.append("limit {limit}");
      queryParams.put("limit", params.limit());
    }
    
    return query;
  }

  public Query build(SearchParameter params) {
    this.params = params;
    StringBuilder query = match()
      .append(where())
      .append(returns())
      .append(orderBy())
      .append(paging());

    return new Query(query.toString(), queryParams);
  }

  public Query buildSimple(SearchParameter params) {
    this.params = params;
    StringBuilder query = match()
      .append(where());

    return new Query(query.toString(), queryParams);
  }
}
