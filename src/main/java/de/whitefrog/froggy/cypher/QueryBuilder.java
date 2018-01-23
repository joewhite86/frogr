package de.whitefrog.froggy.cypher;

import de.whitefrog.froggy.helper.ReflectionUtil;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.relationship.Relationship;
import de.whitefrog.froggy.model.rest.Filter;
import de.whitefrog.froggy.model.rest.SearchParameter;
import de.whitefrog.froggy.persistence.AnnotationDescriptor;
import de.whitefrog.froggy.persistence.FieldDescriptor;
import de.whitefrog.froggy.persistence.ModelCache;
import de.whitefrog.froggy.persistence.Persistence;
import de.whitefrog.froggy.repository.RelationshipRepository;
import de.whitefrog.froggy.repository.Repository;
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
  private final List<String> queryFields = new ArrayList<>();

  public QueryBuilder(Repository repository) {
    this.repository = repository;
    this.type = repository.getModelClass().getSimpleName();
    Persistence.cache().fieldMap(repository.getModelClass()).forEach(descriptor -> {
      if(descriptor.annotations().indexed != null || descriptor.annotations().unique) {
        queryFields.add(descriptor.field().getName());
      }
    });
  }

  public Repository repository() {
    return repository;
  }

  private String id() {
    return repository().queryIdentifier();
  }

  private StringBuilder match() {
    // add required matches for ordered fields when counted
    for(SearchParameter.OrderBy order : params.orderBy()) {
      if(!order.field().contains(".") && !matches.keySet().contains(order.field())) {
        AnnotationDescriptor descriptor =
          Persistence.cache().fieldAnnotations(repository().getModelClass(), order.field());
        if(descriptor.relationshipCount != null) {
          MatchBuilder match = new MatchBuilder()
            .relationship(order.field())
            .relationshipType(descriptor.relationshipCount.type());
          
          if(descriptor.relationshipCount.direction().equals(Direction.OUTGOING)) {
            match.from(id()).fromLabel(type);
            if(!descriptor.relationshipCount.otherModel().equals(Model.class)) 
              match.toLabel(descriptor.relationshipCount.otherModel().getSimpleName());
          } else {
            match.to(id()).toLabel(type);
            if(!descriptor.relationshipCount.otherModel().equals(Model.class))
              match.fromLabel(descriptor.relationshipCount.otherModel().getSimpleName());
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
          Persistence.cache().fieldAnnotations(repository().getModelClass(), returnsKey);
        if(descriptor == null) continue;
        
        if(descriptor.relatedTo != null) {
          boolean isRelationship = false;
          try {
            Field field = ModelCache.getField(repository().getModelClass(), returnsKey);
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
          if(descriptor.relatedTo.direction().equals(Direction.OUTGOING)) {
            match.from(id()).fromLabel(type);
            if(!isRelationship) match.to(returnsKey);
          } else {
            match.to(id()).toLabel(type);
            if(!isRelationship) match.from(returnsKey);
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
    
    FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(clazz, fieldName);
    if(descriptor == null) {
      for(Class sub: Persistence.cache().subTypesOf(clazz)) {
        descriptor = Persistence.cache().fieldDescriptor(sub, fieldName);
        if(descriptor != null) break;
      }
      if(descriptor == null) {
        logger.warn("the field for filter {} could not be found", filter.getProperty());
        return;
      }
    }
    
    AnnotationDescriptor annotations = descriptor.annotations();
    String className = descriptor.baseClass().getSimpleName();

    if(annotations.relatedTo != null) {
      if(annotations.relatedTo.direction().equals(Direction.OUTGOING)) {
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
        if(lookup.contains(".to.") && Persistence.cache().fieldDescriptor(repository().getModelClass(), "to") == null) 
          lookup = lookup.replace(".to", "_to");
        if(lookup.contains(".from.") && Persistence.cache().fieldDescriptor(repository().getModelClass(), "from") == null)
          lookup = lookup.replace(".from", "_from");
        String[] split = lookup.split("\\.");
        lookup = !lookup.contains(".")? 
          id() + "." + lookup: split[split.length - 2] + "." + split[split.length - 1];
        String marker = filter.getProperty().replaceAll("\\.", "") + i;

        if(filter instanceof Filter.Equals) {
          if(filter.getValue() == null) {
            wheres.add(lookup + " IS NULL");
          }
          else {
            String where = "(";
            if(filter.getValue() instanceof Boolean) {
              if(filter.getValue() == Boolean.TRUE) {
                where+= lookup + " IS NOT NULL AND ";
              }
              else {
                where+= lookup + " IS NULL OR ";
              }
            }
            where += lookup + " = {" + marker + "})";

            wheres.add(where);
            queryParams.put(marker, filter.getValue());
          }
        }
        else if(filter instanceof Filter.StartsWith) {
          wheres.add(lookup + " starts with {" + marker + "}");
          queryParams.put(marker, filter.getValue());
        }
        else if(filter instanceof Filter.EndsWith) {
          wheres.add(lookup + " ends with {" + marker + "}");
          queryParams.put(marker, filter.getValue());
        }
        else if(filter instanceof Filter.Contains) {
          wheres.add(lookup + " contains {" + marker + "}");
          queryParams.put(marker, filter.getValue());
        }
        else if(filter instanceof Filter.NotEquals) {
          if(filter.getValue() == null) {
            wheres.add(lookup + " IS NOT NULL");
          }
          else {
            String where = "(" + lookup + " <> {" + marker + "}";
            if(filter.getValue() instanceof Boolean) {
              where += "OR " + lookup + " IS " +
                (filter.getValue() == Boolean.FALSE? "NOT": "") + " NULL";
            }
            wheres.add(where + ")");
            queryParams.put(marker, filter.getValue());
          }
        }
        else if(filter instanceof Filter.GreaterThan) {
          String including = ((Filter.GreaterThan) filter).isIncluding()? "=": "";
          wheres.add(lookup + " >" + including + " {" + marker + "}");
          queryParams.put(marker, filter.getValue());
        }
        else if(filter instanceof Filter.LessThan) {
          String including = ((Filter.LessThan) filter).isIncluding()? "=": "";
          wheres.add(lookup + " <" + including + " {" + marker + "}");
          queryParams.put(marker, filter.getValue());
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
        if(!order.field().contains(".") && Persistence.cache().fieldAnnotations(repository().getModelClass(), order.field()).relationshipCount != null) {
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
      List<String> returns = new ArrayList<>(params.returns());
//      if(Persistence.cache().fieldDescriptor(repository().getModelClass(), "to") == null) {
//        returns = returns.stream().map(r -> {
//          if(r.contains(".to")) return r.replace(".to", "_to");
//          else return r;
//        }).collect(Collectors.toList());
//      }
      returns = returns.stream().map(r -> {
        if(r.contains(".")) return r.replace(".", "_");
        return r; 
      }).collect(Collectors.toList());
      ret.add(StringUtils.join(returns, ","));
    }
    
    // to order by a relationship count we need to count it in return for cypher
    for(SearchParameter.OrderBy order : params.orderBy()) {
      if(!order.field().contains(".")) {
        AnnotationDescriptor descriptor =
          Persistence.cache().fieldAnnotations(repository().getModelClass(), order.field());
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
