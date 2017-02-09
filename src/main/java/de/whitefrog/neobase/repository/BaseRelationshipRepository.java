package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.DefaultResultIterator;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.exception.MissingRequiredException;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.helper.Streams;
import de.whitefrog.neobase.index.IndexUtils;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.AnnotationDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import de.whitefrog.neobase.service.Search;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

public abstract class BaseRelationshipRepository<T extends Relationship> implements RelationshipRepository<T> {
  private final Logger logger;
  private static final Map<Service, Map<String, Index<org.neo4j.graphdb.Relationship>>> indexCache = new HashMap<>();

  private QueryBuilder queryBuilder;

  private final Service service;
  private final String modelName;
  private Class<?> modelClass;

  public BaseRelationshipRepository(Service service) {
    this.logger = LoggerFactory.getLogger(getClass());
    this.modelName = getClass().getSimpleName().substring(0, getClass().getSimpleName().indexOf("Repository"));
    this.service = service;
    this.queryBuilder = new QueryBuilder(this);
  }
  public BaseRelationshipRepository(Service service, String modelName) {
    this.logger = LoggerFactory.getLogger(getClass());
    this.modelName = modelName;
    this.service = service;
    this.queryBuilder = new QueryBuilder(this);
  }

  public Logger logger() {
    return logger;
  }

  @Override
  public boolean contains(T model) {
    return model.getId() != -1 && find(model.getId()) != null;
  }

  public T createModel() {
    try {
      return (T) getModelClass().newInstance();
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
  }
  
  @Override
  public T createModel(PropertyContainer node) {
    return createModel(node, new FieldList());
  }

  @Override
  public T createModel(PropertyContainer node, FieldList fields) {
    return fetch(Persistence.get(node), false, fields);
  }

  public T fetch(T tag, String... fields) {
    return fetch(tag, FieldList.parseFields(fields));
  }

  @Override
  public T fetch(T tag, FieldList fields) {
    return fetch(tag, false, fields);
  }

  @Override
  public T fetch(T tag, boolean refetch, FieldList fields) {
    Relationships.fetch(tag, fields);
    return tag;
  }

  @Override
  public boolean filter(Node node, Collection<Filter> filters) {
    return filters.stream().anyMatch(filter ->
      !node.hasProperty(filter.getProperty()) || !filter.test(node.getProperty(filter.getProperty())));
  }

  @Override
  public T find(long id, String... fields) {
    return find(id, Arrays.asList(fields));
  }

  @Override
  @SuppressWarnings("unchecked")
  public T find(long id, List<String> fields) {
    return find(id, FieldList.parseFields(fields));
  }

  public T find(long id, FieldList fields) {
    try {
      T model = createModel(graph().getRelationshipById(id), fields);
      if(CollectionUtils.isNotEmpty(fields)) fetch(model, fields);
      return model;
    } catch(IllegalStateException e) {
      logger.warn(e.getMessage(), e);
      return null;
    } catch(NotFoundException e) {
      return null;
    }
  }

  @Override
  public Stream<T> find(String property, Object value) {
    return search().filter(new Filter.Equals(property, value)).stream();
  }

  @Override
  public Stream<T> find(SearchParameter params) {
    throw new UnsupportedOperationException("no query possible on this class");
  }

  @Override
  public T findByUuid(String uuid) {
    Optional<T> found = findIndexed(Model.Uuid, uuid).findFirst();
    return found.isPresent()? found.get(): null;
  }

  @Override
  public Stream<T> findIndexed(String field, Object value) {
    return findIndexed(field, value, new SearchParameter());
  }

  @Override
  public Stream<T> findIndexed(String field, Object value, SearchParameter params) {
    return findIndexed(index(), field, value, params);
  }

  @Override
  public Stream<T> findIndexed(Index<org.neo4j.graphdb.Relationship> index, String field, Object value) {
    return findIndexed(index, field, value, new SearchParameter());
  }

  @Override
  public Stream<T> findIndexed(Index<org.neo4j.graphdb.Relationship> index, String field, Object value, SearchParameter params) {
    IndexHits<org.neo4j.graphdb.Relationship> found = IndexUtils.query(index, field,
      value instanceof String? ((String) value).toLowerCase(): value.toString(),
      params.limit() * params.page());
    return Streams.get(new DefaultResultIterator<>(this, found, params));
  }
  
  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = Persistence.cache().getModel(modelName);
    }
    
    return modelClass;
  }

  @Override
  public org.neo4j.graphdb.Relationship getRelationship(Relationship model) {
    Validate.notNull(model, "The model is null");
    Validate.notNull(model.getId(), "ID can not be null.");
    try {
      return service().graph().getRelationshipById(model.getId());
    } catch(NotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public GraphDatabaseService graph() {
    return service().graph();
  }

  @Override
  public Index<org.neo4j.graphdb.Relationship> index() {
    return index(modelName);
  }

  @Override
  public Index<org.neo4j.graphdb.Relationship> index(String indexName) {
    if(!indexCache.containsKey(service())) {
      indexCache.put(service(), new HashMap<>());
    }
    if(!indexCache.get(service()).containsKey(indexName)) {
      Index<org.neo4j.graphdb.Relationship> index = graph().index().forRelationships(indexName, indexConfig(indexName));
      indexCache.get(service()).put(indexName, index);
      logger.debug("lucene index created for \"{}\"", indexName);
    }

    return indexCache.get(service()).get(indexName);
  }

  @Override
  public void index(T model, String name, Object value) {
    index(indexForField(name), model, name, value);
  }

  @Override
  public void index(Index<org.neo4j.graphdb.Relationship> index, T model, String name, Object value) {
    org.neo4j.graphdb.Relationship node = getRelationship(model);
    index.add(node, name, value);
    if(logger.isDebugEnabled()) {
      logger.debug("added \"{}\" with value \"{}\" on \"{}\" index", name, value, index.getName());
    }
  }

  @Override
  public Map<String, String> indexConfig(String index) {
    return MapUtil.stringMap(
      "type", "exact",
      "to_lower_case", "true"
    );
  }

  @Override
  public Index<org.neo4j.graphdb.Relationship> indexForField(String fieldName) {
    return index();
  }

  @Override
  public void indexRemove(org.neo4j.graphdb.Relationship relationship) {
    indexRemove(index(), relationship);
  }

  @Override
  public void indexRemove(Index<org.neo4j.graphdb.Relationship> index, org.neo4j.graphdb.Relationship relationship) {
    index.remove(relationship);
    if(logger.isDebugEnabled()) {
      logger.debug("{} removed from index {}", createModel(relationship), index.getName());
    }
  }

  @Override
  public void indexRemove(org.neo4j.graphdb.Relationship relationship, String field) {
    indexRemove(indexForField(field), relationship, field);
  }

  @Override
  public void indexRemove(Index<org.neo4j.graphdb.Relationship> index, org.neo4j.graphdb.Relationship relationship, String field) {
    index.remove(relationship, field);
    if(logger.isDebugEnabled()) {
      logger.debug("{} removed field \"{}\" from index {}", createModel(relationship), field, index.getName());
    }
  }

  @Override
  public QueryBuilder queryBuilder() {
    return queryBuilder;
  }
  
  @Override
  public String queryIdentifier() {
    return getModelClass().getSimpleName().toLowerCase();
  }
  
  @Override
  public void remove(T model) throws PersistException {
    Validate.notNull(model.getId(), "'id' is required");
    Relationships.delete(model);
    logger.info("{} deleted", model);
  }

  @Override
  public void save(T model) throws PersistException {
    save(new SaveContext<>(this, model));
  }

  @Override
  @SafeVarargs
  public final void save(T... relationships) throws PersistException {
    for(T relationship : relationships) {
      save(relationship);
    }
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    // TODO: Fix
//    Relationships.save(this, context);
    logger.info("{} saved", context.model());
  }
  
  @Override
  public Search search() {
    return new Search(this);
  }

  @Override
  public Service service() {
    return service;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void sort(List<T> list, List<SearchParameter.OrderBy> orderBy) {
    if(orderBy != null && !list.isEmpty()) {
      Class<? extends Base> clazz = list.get(0).getClass();
      final List<Field> orderedFields = new ArrayList<>(orderBy.size());
      for(SearchParameter.OrderBy order : orderBy) {
        try {
          final Field field = ReflectionUtil.getSuperField(clazz, order.field());
          final String dir = order.dir();
          if(!field.isAccessible()) field.setAccessible(true);
          Collections.sort(list, (o1, o2) -> {
            try {
              // just proceed if the already ordered fields aren't equal
              if(!orderedFields.isEmpty()) {
                boolean inOrder = false;
                for(Field orderedField : orderedFields) {
                  Object val1 = orderedField.get(o1);
                  Object val2 = orderedField.get(o2);
                  if(val1 != val2 && (val1 == null || val2 == null)) {
                    inOrder = true;
                    break;
                  }
                  int compare = val1 == null? 1: ((Comparable) val1).compareTo(val2);
                  if(compare != 0) {
                    inOrder = true;
                    break;
                  }
                }
                if(inOrder) return 0;
              }
              Object val1 = field.get(o1);
              Object val2 = field.get(o2);
              if(dir.equalsIgnoreCase("asc")) {
                if(val1 == null) return -1;
                else if(val2 == null) return 1;
                return ((Comparable) val1).compareTo(val2);
              } else {
                if(val2 == null) return -1;
                else if(val1 == null) return 1;
                return ((Comparable) val2).compareTo(val1);
              }
            } catch(IllegalAccessException e) {
              logger.error("field " + field.getName() + ", used for sorting, is not accessible");
            }
            return 0;
          });
          orderedFields.add(field);
        } catch(NoSuchFieldException e) {
          logger.warn("couldn't sort by field " + order.field() + ", field does not exist on class " + clazz.getName());
        }
      }
    }
  }

  public void validateModel(SaveContext<T> context) {
    context.fieldMap().forEach(f -> {
      if(context.model().getCheckedFields().contains(f.getName())) return;
      AnnotationDescriptor annotations = Persistence.cache().fieldAnnotations(context.model().getClass(), f.getName());
      if(!context.model().isPersisted() && annotations.required) {
        try {
          Object value = f.field().get(context.model());
          if(value == null || (value instanceof String && ((String) value).isEmpty())) {
            throw new MissingRequiredException(context.model(), f.field());
          }
        } catch(IllegalAccessException e) {
          logger.error(e.getMessage(), e);
        }
      }
      Set<ConstraintViolation<T>> violations = service().validator().validateProperty(context.model(), f.getName());
      for(ConstraintViolation<T> violation : violations) {
        logger.error(violation.getPropertyPath().toString() + " " + violation.getMessage());
      }
      if(CollectionUtils.isNotEmpty(violations)) {
        throw new javax.validation.ConstraintViolationException("violations storing " + context.model(), violations);
      }
    });
  }

  @Override
  public void dispose() {
    
  }
}
