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
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class BaseQueryBuilder implements QueryBuilder {
  private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
  private Repository repository;
  private String id = "e";
  private List<String> queryFields = new ArrayList<>();

  public BaseQueryBuilder(Repository repository) {
    this.repository = repository;
    Persistence.cache().fieldMap(repository.getModelClass()).forEach(descriptor -> {
      if(descriptor.annotations().indexed != null || descriptor.annotations().unique) {
        queryFields.add(descriptor.field().getName());
      }
    });
  }

  @Override
  public StringBuilder start(SearchParameter params, Map<String, Object> queryParams) {
    StringBuilder query = new StringBuilder();
    if(!params.ids().isEmpty()) {
      query.append("start ")
        .append(id).append("=node(").append(StringUtils.join(params.ids(), ",")).append(") ");
    }
    else if(!params.uuids().isEmpty()) {
      query.append("start ")
        .append(id).append("=node:").append(repository().label())
        .append("(\"uuid: ").append(StringUtils.join(params.uuids(), " uuid: ")).append("\") ");
    }
    else if(params.query() != null) {
      if(params.query().trim().isEmpty()) {
        throw new IllegalArgumentException("empty query is not allowed");
      }
      query.append("start e=node:").append(repository().index().getName()).append("({query}) ");
      if(params.query().contains(":")) {
        String[] split = params.query().split(":", 2);
        if(split[1].isEmpty()) {
          throw new IllegalArgumentException("empty queries not allowed: \"" + params.query() + "\"");
        }
        queryParams.put("query", split[0] + ":" + QueryParser.escape(split[1])
          .replace(" ", "\\ ")
          .replace("\\*", "*"));
      } else {
        List<String> queryStrings = new ArrayList<>(queryFields.size());
        queryStrings.addAll(queryFields.stream()
          .map(field -> field + ": " + QueryParser.escape(params.query().toLowerCase())
            .replace(" ", "\\ ")
            .replace("\\*", "*")).collect(Collectors.toList()));
        queryParams.put("query", StringUtils.join(queryStrings, " OR "));
      }
    }
    
    return query;
  }

  public Repository repository() {
    return repository;
  }

  public String id() {
    return id;
  }
  
  @Override
  public StringBuilder match(SearchParameter params, Map<String, Object> queryParams) {
    StringBuilder match = new StringBuilder("(").append(id);
    StringBuilder start = start(params, queryParams);
    // only append label if there is no legacy index lookup
    if(start.length() == 0) {
      match.append(":").append(repository.label().name());
    }
    match.append(")");

    // add required matches for ordered fields when counted
    Map<String, String> matches = new HashMap<>();
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
    for(SearchParameter.PropertyFilter filter : params.filters()) {
      String fieldName = filter.property();
      if(fieldName.contains(".")) fieldName = fieldName.substring(0, fieldName.indexOf("."));
      if(matches.keySet().contains(fieldName)) continue;

      FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(repository().getModelClass(), fieldName);
      if(descriptor == null) {
        throw new QueryParseException("the field for filter " + filter.property() + " could not be found",
          new ReflectiveOperationException());
      }
      
      AnnotationDescriptor annotations = descriptor.annotations();
      String className = descriptor.baseClass().getSimpleName();
      
      if(annotations.relatedTo != null) {
        StringBuilder newMatch = new StringBuilder(match);        
        if(!descriptor.isRelationship()) {
          newMatch.append(annotations.relatedTo.direction().equals(Direction.OUTGOING)? "-": "<-");
          newMatch.append("[:").append(annotations.relatedTo.type()).append("]");
          newMatch.append(annotations.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
          newMatch.append("(").append(fieldName).append(":").append(className).append(")");
        } else {
          boolean isTo = filter.property().contains(".to.");
          newMatch.append(annotations.relatedTo.direction().equals(Direction.OUTGOING)? "-": "<-");
          newMatch.append("[").append(!isTo? fieldName: "").append(":").append(annotations.relatedTo.type()).append("]");
          newMatch.append(annotations.relatedTo.direction().equals(Direction.INCOMING)? "-": "->");
          newMatch.append("(").append(isTo? fieldName: "").append(")");
        }
        matches.put(fieldName, newMatch.toString());
      }
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
    
    if(matches.isEmpty() && start.length() > 0) return new StringBuilder();
    else if(matches.isEmpty()) return new StringBuilder(" match (" + id + ":" + repository().label() + ")");
    return new StringBuilder(" match ").append(StringUtils.join(matches.values(), ", ")).append(" ");
  }

  @Override
  public StringBuilder where(SearchParameter params, Map<String, Object> queryParams) {
    StringBuilder query = new StringBuilder();
    if(params.isFiltered()) {
      List<String> wheres = new ArrayList<>(3);
      int i = 0;
      for(SearchParameter.PropertyFilter filter : params.filters()) {
        String fieldName = filter.property();
        if(fieldName.contains(".")) fieldName = fieldName.substring(0, fieldName.indexOf("."));
        AnnotationDescriptor descriptor =
          Persistence.cache().fieldAnnotations(repository().getModelClass(), fieldName);
        // only write a where clause for properties, not for relationships
        // this should be handled by the "match" method
        if(descriptor != null) {
          String lookup = descriptor.relatedTo == null? id + "." + filter.property(): filter.property();
          String marker = filter.property().replaceAll("\\.", "") + i;
          
          if(lookup.contains(".to.")) lookup = lookup.replace(".to.", ".");

          if(filter.getFilter() instanceof Filter.Equals) {
            if(filter.getFilter().getValue() == null) {
              wheres.add(lookup + " IS NULL");
            }
            else {
              String where = "";
              if(filter.getFilter().getValue() instanceof Boolean) {
                if(filter.getFilter().getValue() == Boolean.TRUE) {
                  where = lookup + " IS NOT NULL AND ";
                }
                else {
                  where = lookup + " IS NULL OR ";
                }
              }
              where += lookup + " = {" + marker + "}";

              wheres.add(where);
              queryParams.put(marker, filter.getFilter().getValue());
            }
          }
          else if(filter.getFilter() instanceof Filter.NotEquals) {
            if(filter.getFilter().getValue() == null) {
              wheres.add(lookup + " IS NOT NULL");
            }
            else {
              String where = lookup + " <> {" + marker + "}";
              if(filter.getFilter().getValue() instanceof Boolean) {
                where += "OR " + lookup + " IS " +
                  (filter.getFilter().getValue() == Boolean.FALSE? "NOT": "") + " NULL";
              }
              wheres.add(where);
              queryParams.put(marker, filter.getFilter().getValue());
            }
          }
          else if(filter.getFilter() instanceof Filter.GreaterThan) {
            String including = ((Filter.GreaterThan) filter.getFilter()).isIncluding()? "=": "";
            wheres.add(lookup + " >" + including + " {" + marker + "}");
            queryParams.put(marker, filter.getFilter().getValue());
          }
          else if(filter.getFilter() instanceof Filter.LessThan) {
            String including = ((Filter.LessThan) filter.getFilter()).isIncluding()? "=": "";
            wheres.add(lookup + " <" + including + " {" + marker + "}");
            queryParams.put(marker, filter.getFilter().getValue());
          }
          else if(filter.getFilter() instanceof Filter.Range) {
            String including = ((Filter.Range) filter.getFilter()).isIncluding()? "=": "";
            Filter.Range range = (Filter.Range) filter.getFilter();
            wheres.add(lookup + " >" + including + " {" + marker + "_from}");
            wheres.add(lookup + " <" + including + " {" + marker + "_to}");
            queryParams.put(marker + "_from", range.getFrom());
            queryParams.put(marker + "_to", range.getTo());
          }
          i++;
        }
      }
      if(!wheres.isEmpty()) {
        query.append(" where ").append(StringUtils.join(wheres, " AND "));
      }
    }
    return query;
  }

  @Override
  public StringBuilder orderBy(SearchParameter params) {
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
          orders.add(id + "." + order.field() + " " + order.dir());
        }
      }
      query.append(" order by ").append(StringUtils.join(orders, ", "));
    }
    
    return query;
  }

  public StringBuilder returns(SearchParameter params) {
    List<String> ret = new ArrayList<>();
    ret.add(CollectionUtils.isEmpty(params.returns())? "distinct " + id: StringUtils.join(params.returns(), ","));
    
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
    
    return new StringBuilder(" return ").append(StringUtils.join(ret, ", "));
  }

  @Override
  public Query build(SearchParameter params) {
    Map<String, Object> queryParams = new HashMap<>();
    StringBuilder query = new StringBuilder();

    query.append(start(params, queryParams));

    query.append(match(params, queryParams));

    query.append(where(params, queryParams));

    query.append(returns(params));

    query.append(orderBy(params));

    if(params.page() > 1) {
      query.append(" skip {skip} ");
      queryParams.put("skip", (params.page() - 1) * params.limit());
    }

    if(params.limit() < Integer.MAX_VALUE) {
      query.append(" limit {limit}");
      queryParams.put("limit", params.limit());
    }

    return new Query(query.toString(), queryParams);
  }

  @Override
  public Number sum(String field, SearchParameter params) {
    Map<String, Object> queryParams = new HashMap<>();
    String q = "" +
      start(params, queryParams) +
      match(params, queryParams) +
      where(params, queryParams);

//    if(params.returns() != null) {
//      q+= " return count(" + params.returns() + ") as c";
//    } else {
    q+= " return sum(" + field + ") as c";
//    }

    Query query = new Query(q, queryParams);
    Result result = repository.service().graph().execute(query.query(), query.params());

    logger.debug(params.toString());
    logger.debug(query.query());
    logger.debug(query.params().toString());

    return (Number) result.columnAs("c").next();
  }

  @Override
  public long count(SearchParameter params) {
    Map<String, Object> queryParams = new HashMap<>();
    String q = "" +
      start(params, queryParams) +
      match(params, queryParams) +
      where(params, queryParams);
    
//    if(params.returns() != null) {
//      q+= " return count(" + params.returns() + ") as c";
//    } else {
      q+= " return count(*) as c";
//    }

    Query query = new Query(q, queryParams);
    try {
      Result result = repository.service().graph().execute(query.query(), query.params());

      logger.debug(params.toString());
      logger.debug(query.query());
      logger.debug(query.params().toString());

      return (long) result.columnAs("c").next();
    } catch(IllegalStateException e) {
      logger.error("On query: " + query.query(), e);
      throw e;
    }
  }

  @Override
  public Result execute(SearchParameter params) {
    Query query = build(params);
    logger.debug(params.toString());
    logger.debug(query.query());
    logger.debug(query.params().toString());
    try {
      return repository.service().graph().execute(query.query(), query.params());
    } catch(IllegalStateException e) {
      logger.error("On query: " + query.query(), e);
      throw e;
    }
  }
}
