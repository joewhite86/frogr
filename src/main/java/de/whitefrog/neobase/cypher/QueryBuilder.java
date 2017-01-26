package de.whitefrog.neobase.cypher;

import de.whitefrog.neobase.exception.QueryParseException;
import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.AnnotationDescriptor;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.ModelCache;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.neo4j.graphdb.Direction;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

public class QueryBuilder {
  private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
  private static final boolean insertNewLine = false;
  
  private Repository repository;
  private boolean hasStart = false;
  private final Map<String, Object> queryParams = new HashMap<>();
  private final List<String> queryFields = new ArrayList<>();
  private final List<String> usedInStart = new ArrayList<>();
  private SearchParameter params;
  private final String type;
  private final Map<String, String> matches = new HashMap<>();

  public QueryBuilder(Repository repository) {
    this.repository = repository;
    this.type = repository.getModelClass().getSimpleName();
    Persistence.cache().fieldMap(repository.getModelClass()).forEach(descriptor -> {
      if(descriptor.annotations().indexed != null || descriptor.annotations().unique) {
        queryFields.add(descriptor.field().getName());
      }
    });
  }

  private String escape(String query) {
    return QueryParser.escape(query)
      .replace(" ", "\\ ")
      .replace("\\*", "*");
  }
  
  public StringBuilder start() {
    StringBuilder query = new StringBuilder();
    if(CollectionUtils.isNotEmpty(params.ids())) {
      query.append(id()).append("=node(").append(StringUtils.join(params.ids(), ",")).append(")");
      hasStart = true;
    }
    else if(CollectionUtils.isNotEmpty(params.uuids())) {
      query.append(id()).append("=node:").append(type).append("({query})");
      queryParams.put("query", "uuid:" + StringUtils.join(params.uuids(), " uuid:"));
      hasStart = true;
    }
    else if(params.query() != null) {
      if(params.query().trim().isEmpty()) {
        throw new IllegalArgumentException("empty query is not allowed");
      }
      hasStart = true;
      query.append(id()).append("=node:").append(type).append("({query})");
      if(params.query().contains(":")) {
        String[] split = params.query().split(":", 2);
        if(split[1].isEmpty()) {
          throw new IllegalArgumentException("empty queries not allowed: \"" + params.query() + "\"");
        }
        queryParams.put("query", split[0] + ":" + QueryParser.escape(split[1])
          .replace(" ", "\\ ")
          .replace("\\*", "*"));
      } else {
        List<String> queryStrings = queryFields.stream()
          .map(field -> field + ": " + escape(params.query().toLowerCase()))
          .collect(Collectors.toList());
        queryParams.put("query", StringUtils.join(queryStrings, " OR "));
      }
    }
    if(params.isFiltered()) {
      Map<String, List<String>> queryStrings = new HashMap<>();
      Map<String, String> starts = new HashMap<>();
      
      for(Filter filter: params.filters()) {
        if(!(filter instanceof Filter.Equals) || filter.getValue() == null || 
            filter.getValue() instanceof Boolean ) continue;
        getStartSub(starts, queryStrings, filter, repository.getModelClass(), filter.getProperty());
      }
      if(!queryStrings.isEmpty()) {
        for(String id: starts.keySet()) {
          if(query.length() != 0) query.append(", ");
          if(id.equals(id())) hasStart = true;
          query.append(id).append("=node:").append(starts.get(id)).append("({query_").append(id).append("})");
          queryParams.put("query_" + id, StringUtils.join(queryStrings.get(id), " OR "));
        }
      }
    }
    
    return query.length() == 0? query: new StringBuilder("start ").append(query).append(insertNewLine? "\n": " ");
  }
  
  private void getStartSub(Map<String, String> starts, Map<String, List<String>> queryStrings, 
                                    Filter filter, Class<?> modelClass, String path) {
    // loop until correct search is reached
    String fieldName = path.contains(".")? path.substring(0, path.indexOf(".")): path;
    FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(modelClass, fieldName);
    if(descriptor == null) {
      for(Class sub: Persistence.cache().subTypesOf(modelClass)) {
        descriptor = Persistence.cache().fieldDescriptor(sub, fieldName);
        if(descriptor != null) break;
      }
      if(descriptor == null) {
        logger.warn("field {} not found in {}", fieldName, modelClass);
        return;
      }
    }
    AnnotationDescriptor annotations = descriptor.annotations();
    
    if(path.contains(".") && path.substring(fieldName.length() + 1).contains(".")) {
      getStartSub(starts, queryStrings, filter, descriptor.baseClass(), path.substring(path.indexOf(".") + 1));
      return;
    }

    if(!path.contains(".")) {
      if(annotations.indexed != null || annotations.unique) {
        if(!starts.containsKey(id())) starts.put(id(), type);
        if(!queryStrings.containsKey(id())) queryStrings.put(id(), new ArrayList<>());
        usedInStart.add(filter.getProperty());
        queryStrings.get(id()).add(filter.getProperty() + ":" +
          escape(filter.getValue().toString().toLowerCase()));
      }
    } else if(annotations.relatedTo != null) {
      String queryField = path.substring(path.indexOf(".") + 1);
      Class clazz = descriptor.baseClass();
      if(descriptor.isRelationship()) {
        ParameterizedType type = (ParameterizedType) descriptor.field().getGenericType();
        clazz = (Class<?>) type.getActualTypeArguments()[1];
      }
      FieldDescriptor subDescriptor = Persistence.cache().fieldDescriptor(clazz, queryField);
      if(subDescriptor == null || 
          (subDescriptor.annotations().indexed == null && !subDescriptor.annotations().unique)) return;
      if(!starts.containsKey(fieldName)) starts.put(fieldName, clazz.getSimpleName());
      if(!queryStrings.containsKey(fieldName)) queryStrings.put(fieldName, new ArrayList<>());
      usedInStart.add(filter.getProperty());
      queryStrings.get(fieldName).add(queryField + ":" +
        escape(filter.getValue().toString().toLowerCase()));
    }
  }

  public Repository repository() {
    return repository;
  }

  public String id() {
    return repository().queryIdentifier();
  }
  
  public StringBuilder match() {
    StringBuilder match = new StringBuilder("(").append(id());
    // only append label if there is no legacy index lookup
    if(!hasStart && usedInStart.isEmpty()) {
      match.append(":").append(type);
    }
    match.append(")");

    // add required matches for ordered fields when counted
    for(SearchParameter.OrderBy order : params.orderBy()) {
      if(!order.field().contains(".") && !matches.keySet().contains(order.field())) {
        AnnotationDescriptor descriptor =
          Persistence.cache().fieldAnnotations(repository().getModelClass(), order.field());
        if(descriptor.relationshipCount != null) {
          StringBuilder newMatch = new StringBuilder(match);
          newMatch.append(descriptor.relationshipCount.direction().equals(Direction.OUTGOING)? "-": "<-");
          newMatch.append("[").append(order.field()).append(":").append(descriptor.relationshipCount.type()).append("]");
          newMatch.append(descriptor.relationshipCount.direction().equals(Direction.INCOMING)? "-": "->");
          if(!descriptor.relationshipCount.otherModel().equals(Model.class)) {
            newMatch.append("(:").append(descriptor.relationshipCount.otherModel().getSimpleName()).append(")");
          }
          else {
            newMatch.append("()");
          }
          matches.put(order.field(), newMatch.toString());
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

          StringBuilder newMatch = new StringBuilder(match);
          newMatch.append(descriptor.relatedTo.direction().equals(Direction.OUTGOING)? "-": "<-");
          if(isRelationship) {
            newMatch.append("[").append(returnsKey).append(":").append(descriptor.relatedTo.type()).append("]");
            newMatch.append(descriptor.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
            newMatch.append("()");
          }
          else {
            newMatch.append("[:").append(descriptor.relatedTo.type()).append("]");
            newMatch.append(descriptor.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
            newMatch.append("(").append(returnsKey).append(")");
          }
          matches.put(returnsKey, newMatch.toString());
        }
      }
    }
    
    if(matches.isEmpty() && hasStart) return new StringBuilder();
    else if(matches.isEmpty()) return new StringBuilder("match (" + id() + ":" + type + ")").append(insertNewLine? "\n": " ");
    return new StringBuilder("match ").append(StringUtils.join(matches.values(), ", ")).append(insertNewLine? "\n": " ");
  }
  
  private String getMatchName(String property) {
    String matchName = property.replace(".to", "");
    return matchName.contains(".")?
      matchName.substring(0, matchName.lastIndexOf(".")): matchName;
  }
  
  private void generateFilterMatch(Filter filter, Class<?> clazz, String id, String fieldName) {
    StringBuilder match = new StringBuilder("(").append(id);
    // only append label if there is no legacy index lookup
    if(!hasStart && id.equals(id())) {
      match.append(":").append(type);
    }
    match.append(")");
    
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
      StringBuilder newMatch = new StringBuilder(match);
      if(!descriptor.isRelationship()) {
        newMatch.append(annotations.relatedTo.direction().equals(Direction.OUTGOING)? "-": "<-");
        newMatch.append("[:").append(annotations.relatedTo.type()).append("]");
        newMatch.append(annotations.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
        newMatch.append("(").append(fieldName);
        // if the filter is already used as start clause, we don't need to append the class name
        if(!usedInStart.contains(filter.getProperty())) newMatch.append(":").append(className);
        newMatch.append(")");
      } else {
        newMatch.append(annotations.relatedTo.direction().equals(Direction.OUTGOING)? "-": "<-");
        newMatch.append("[").append(fieldName).append(":").append(annotations.relatedTo.type()).append("]");
        newMatch.append(annotations.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
        newMatch.append("(").append(fieldName).append("_to").append(")");
      }
      String sub = filter.getProperty().substring(fieldName.length() + filter.getProperty().indexOf(fieldName) + 1);
      if(sub.contains(".")) {
        generateFilterMatch(filter, descriptor.baseClass(), fieldName, sub.substring(0, sub.indexOf(".")));
      }
//      matches.put(getMatchName(filter.getProperty()), newMatch.toString());
      matches.put(fieldName, newMatch.toString());
    }
  }

  public StringBuilder where() {
    StringBuilder query = new StringBuilder();
    if(params.isFiltered()) {
      List<String> wheres = new ArrayList<>(3);
      int i = 0;
      for(Filter filter : params.filters()) {
        if(usedInStart.contains(filter.getProperty())) continue;
        String lookup = filter.getProperty();
        if(Persistence.cache().fieldDescriptor(repository().getModelClass(), "to") == null && 
            lookup.contains(".to.")) lookup = lookup.replace(".to", "_to");
        String[] split = lookup.split("\\.");
        lookup = !lookup.contains(".")? 
          id() + "." + lookup: split[split.length - 2] + "." + split[split.length - 1];
        String marker = filter.getProperty().replaceAll("\\.", "") + i;

        if(filter instanceof Filter.Equals) {
          if(filter.getValue() == null) {
            wheres.add(lookup + " IS NULL");
          }
          else {
            String where = "";
            if(filter.getValue() instanceof Boolean) {
              if(filter.getValue() == Boolean.TRUE) {
                where = lookup + " IS NOT NULL AND ";
              }
              else {
                where = lookup + " IS NULL OR ";
              }
            }
            where += lookup + " = {" + marker + "}";

            wheres.add(where);
            queryParams.put(marker, filter.getValue());
          }
        }
        else if(filter instanceof Filter.NotEquals) {
          if(filter.getValue() == null) {
            wheres.add(lookup + " IS NOT NULL");
          }
          else {
            String where = lookup + " <> {" + marker + "}";
            if(filter.getValue() instanceof Boolean) {
              where += "OR " + lookup + " IS " +
                (filter.getValue() == Boolean.FALSE? "NOT": "") + " NULL";
            }
            wheres.add(where);
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
      if(!wheres.isEmpty()) {
        query.append("where ").append(StringUtils.join(wheres, " AND ")).append(insertNewLine? "\n": " ");
      }
    }
    return query;
  }

  public StringBuilder orderBy() {
    StringBuilder query = new StringBuilder();
    
    if(!params.orderBy().isEmpty()) {
      List<String> orders = new ArrayList<>(params.orderBy().size());
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
      query.append(" order by ").append(StringUtils.join(orders, ", ")).append(insertNewLine? "\n": " ");
    }
    
    return query;
  }

  public StringBuilder returns() {
    List<String> ret = new ArrayList<>();
    
    if(CollectionUtils.isEmpty(params.returns())) {
      ret.add(id());
    } else {
      List<String> returns = new ArrayList<>(params.returns());
      if(Persistence.cache().fieldDescriptor(repository().getModelClass(), "to") == null) {
        returns.forEach(r -> {
          if(r.contains(".to")) r = r.replace(".to", "_to");
        });
      }
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
    
    return new StringBuilder("return ").append(StringUtils.join(ret, ", ")).append(insertNewLine? "\n": " ");
  }
  
  public StringBuilder paging() {
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
    StringBuilder query = start()
      .append(match())
      .append(where())
      .append(returns())
      .append(orderBy())
      .append(paging());

    return new Query(query.toString(), queryParams);
  }

  public Query buildSimple(SearchParameter params) {
    this.params = params;
    StringBuilder query = start()
      .append(match())
      .append(where());

    return new Query(query.toString(), queryParams);
  }
}
