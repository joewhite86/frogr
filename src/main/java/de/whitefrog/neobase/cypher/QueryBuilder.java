package de.whitefrog.neobase.cypher;

import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.AnnotationDescriptor;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.ModelCache;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.RelationshipRepository;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public class QueryBuilder {
  private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
  private static final boolean insertNewLine = false;
  
  private Repository repository;
  private final Map<String, Object> queryParams = new HashMap<>();
  private SearchParameter params;
  private final String type;
  private final Map<String, String> matches = new HashMap<>();

  public QueryBuilder(Repository repository) {
    this.repository = repository;
    this.type = repository.getModelClass().getSimpleName();
  }

  public Repository repository() {
    return repository;
  }

  public String id() {
    return repository().queryIdentifier();
  }
  
  public StringBuilder match() {
    StringBuilder match = new StringBuilder("(").append(id())
      .append(":").append(type).append(")");

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
    
    if(matches.isEmpty()) {
      if(repository() instanceof RelationshipRepository) {
        return new StringBuilder("match ()-[" + id() + ":" + type + "]-()")
          .append(insertNewLine? "\n": " ");
      } else {
        return new StringBuilder("match (" + id() + ":" + type + ")")
          .append(insertNewLine? "\n": " ");
      }
    } else {
      return new StringBuilder("match ")
        .append(StringUtils.join(matches.values(), ", "))
        .append(insertNewLine? "\n": " ");
    }
  }
  
  private void generateFilterMatch(Filter filter, Class<?> clazz, String id, String fieldName) {
    StringBuilder match = new StringBuilder("(").append(id);
    // only append label if there is no legacy index lookup
    if(id.equals(id())) {
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
        newMatch.append(":").append(className);
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
