package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.*;
import de.whitefrog.neobase.collection.ListIterator;
import de.whitefrog.neobase.cypher.BaseQueryBuilder;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.exception.RepositoryInstantiationException;
import de.whitefrog.neobase.exception.TypeMismatchException;
import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.index.IndexUtils;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseRepository<T extends Model> implements Repository<T> {
  private final Logger logger;
  private static final Map<Service, Map<String, Index<Node>>> indexCache = new HashMap<>();

  private QueryBuilder queryBuilder;

  private final Label label;
  private Set<Label> labels;
  private Service service;
  private Class<?> modelClass;

  public BaseRepository(Service service) {
    this.logger = LoggerFactory.getLogger(getClass());
    String modelName = getClass().getSimpleName().substring(0, getClass().getSimpleName().indexOf("Repository"));
    this.label = Label.label(modelName);

    Class modelClass = Persistence.cache().getModel(modelName);
    this.labels = getModelInterfaces(modelClass).stream()
      .map(Label::label)
      .collect(Collectors.toSet());
    this.service = service;
    this.queryBuilder = new BaseQueryBuilder(this);
  }

  public BaseRepository(Service service, String modelName) {
    this.logger = LoggerFactory.getLogger(getClass());
    this.label = Label.label(modelName);

    Class modelClass = Persistence.cache().getModel(modelName);
    this.labels = getModelInterfaces(modelClass).stream()
      .map(Label::label)
      .collect(Collectors.toSet());
    this.service = service;
    this.queryBuilder = new BaseQueryBuilder(this);
  }
  
  private Set<String> getModelInterfaces(Class<?> clazz) {
    Set<String> output = new HashSet<>();
    Class<?>[] interfaces = clazz.getInterfaces();
    for(Class<?> i: interfaces) {
      if(Model.class.isAssignableFrom(i) && !i.equals(Model.class)) {
        output.add(i.getSimpleName());
        output.addAll(getModelInterfaces(i));
      }
    }
    return output;
  }

  public Logger logger() {
    return logger;
  }

  @Override
  public boolean contains(T model) {
    return model.getId() != -1 && find(model.getId()) != null;
  }

  @Override
  public long count() {
    try(Result results = graph().execute("match (n:" + label() + ") return count(*) as c")) {
      Iterator<Long> iterator = results.columnAs("c");
      return iterator.next();
    }
  }

  @Override
  public long count(SearchParameter params) {
    return queryBuilder().count(params);
  }

  public T createModel() {
    try {
      return (T) getModelClass().newInstance();
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
  }
  
  @Override
  public T createModel(Node node) {
    return createModel(node, new FieldList());
  }

  @Override
  public T createModel(Node node, FieldList fields) {
    if(!checkType(node)) {
      throw new TypeMismatchException(node, label());
    }

    return fetch(Persistence.get(node), false, fields);
  }

  boolean checkType(Node node) {
    return node.hasLabel(label());
  }

  @Override
  public T fetch(T tag) {
    return fetch(tag, new FieldList());
  }

  public T fetch(T tag, String... fields) {
    return fetch(tag, FieldList.parseFields(fields));
  }

  @Override
  public T fetch(T tag, boolean refetch, String... fields) {
    return fetch(tag, refetch, FieldList.parseFields(fields));
  }

  @Override
  public T fetch(T tag, FieldList fields) {
    return fetch(tag, false, fields);
  }

  @Override
  public T fetch(T tag, boolean refetch, FieldList fields) {
    Persistence.fetch(tag, fields, refetch);
    return tag;
  }

  @Override
  public void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship) {
    fetch(relationship, new FieldList());
  }

  @Override
  public void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship, String... fields) {
    fetch(relationship, FieldList.parseFields(fields));
  }
  
  @Override
  public void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship, FieldList fields) {
    Relationships.fetch(relationship, fields);
  }

  @Override
  public boolean filter(Node node, Collection<SearchParameter.PropertyFilter> filters) {
    return filters.stream().anyMatch(filter ->
      !node.hasProperty(filter.property()) || !filter.getFilter().test(node.getProperty(filter.property())));
  }

  @Override
  public ResultIterator<T> findAll() {
    return findAll(Integer.MAX_VALUE, 1);
  }

  @Override
  public ResultIterator<T> findAll(int limit, int page) {
    return search(new SearchParameter(page, limit));
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
      T model = createModel(graph().getNodeById(id), fields);
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
  public ResultIterator<T> find(String property, Object value) {
    ResourceIterator<Node> found = graph().findNodes(label(), property, value);
    return new NodeIterator<>(this, found);
  }

  @Override
  public ResultIterator<T> find(SearchParameter params) {
    throw new UnsupportedOperationException("no query possible on this class");
  }

  @Override
  public T findByUuid(String uuid) {
    return findIndexedSingle(Model.Uuid, uuid);
  }

  @Override
  public ResultIterator<T> findChangedSince(long timestamp, int limit, int page) {
    return search(new SearchParameter(page, limit)
      .filter(T.LastModified, new Filter.GreaterThan(timestamp))
      .filter(T.Created, new Filter.GreaterThan(timestamp)));
  }

  @Override
  public ResultIterator<T> findIndexed(String field, Object value) {
    return findIndexed(field, value, new SearchParameter());
  }

  @Override
  public ResultIterator<T> findIndexed(String field, Object value, SearchParameter params) {
    return findIndexed(index(), field, value, params);
  }

  @Override
  public ResultIterator<T> findIndexed(Index<Node> index, String field, Object value) {
    return findIndexed(index, field, value, new SearchParameter());
  }

  @Override
  public ResultIterator<T> findIndexed(Index<Node> index, String field, Object value, SearchParameter params) {
    IndexHits<Node> found = IndexUtils.query(index, field,
      value instanceof String? ((String) value).toLowerCase(): value.toString(),
      params.limit() * params.page());
    return new NodeIterator<>(this, found, params);
  }

  @Override
  public T findIndexedSingle(String field, Object value) {
    return findIndexedSingle(field, value, new SearchParameter());
  }

  @Override
  public T findIndexedSingle(String field, Object value, SearchParameter params) {
    return findIndexedSingle(index(), field, value, params);
  }

  @Override
  public T findIndexedSingle(Index<Node> index, String field, Object value) {
    return findIndexedSingle(index, field, value, new SearchParameter());
  }

  @Override
  public T findIndexedSingle(Index<Node> index, String field, Object value, SearchParameter params) {
    Node node = IndexUtils.querySingle(index, field,
      value instanceof String? value.toString().toLowerCase(): value);
    if(node != null) {
      
    }
    return node == null? null: createModel(node, params.fieldList());
  }

  @Override
  public T findSingle(String property, Object value) {
    T model = null;
    Node node = graph().findNode(label(), property, value);
    if(node != null) model = createModel(node);
    return model;
  }
  
  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = Persistence.cache().getModel(label().name());
    }
    
    return modelClass;
  }

  @Override
  public Node getNode(Model model) {
    Validate.notNull(model, "The model is null");
    Validate.notNull(model.getId(), "ID can not be null.");
    try {
      return service().graph().getNodeById(model.getId());
    } catch(NotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Override
  public GraphDatabaseService graph() {
    return service().graph();
  }

  @Override
  public Index<Node> index() {
    return index(label().name());
  }

  @Override
  public Index<Node> index(String indexName) {
    if(!indexCache.containsKey(service())) {
      indexCache.put(service(), new HashMap<>());
    }
    if(!indexCache.get(service()).containsKey(indexName)) {
      Index<Node> index = graph().index().forNodes(indexName, indexConfig(indexName));
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
  public void index(Index<Node> index, T model, String name, Object value) {
    Node node = getNode(model);
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
  public Index<Node> indexForField(String fieldName) {
    return index();
  }

  @Override
  public void indexRemove(Node node) {
    indexRemove(index(), node);
  }

  @Override
  public void indexRemove(Index<Node> index, Node node) {
    index.remove(node);
    if(logger.isDebugEnabled()) {
      logger.debug("{} removed from index {}", createModel(node), index.getName());
    }
  }

  @Override
  public void indexRemove(Node node, String field) {
    indexRemove(indexForField(field), node, field);
  }

  @Override
  public void indexRemove(Index<Node> index, Node node, String field) {
    index.remove(node, field);
    if(logger.isDebugEnabled()) {
      logger.debug("{} removed field \"{}\" from index {}", createModel(node), field, index.getName());
    }
  }

  @Override
  public Label label() {
    return label;
  }
  
  @Override
  public Set<Label> labels() {
    return labels;
  }

  @Override
  public ResultIterator<T> query(String query) {
    Result results = graph().execute(query);
    return new ExecutionResultIterator<>(this, results, new SearchParameter());
  }

  @Override
  public QueryBuilder queryBuilder() {
    return queryBuilder;
  }
  
  @Override
  public String queryIdentifier() {
    return label().name().toLowerCase();
  }
  
  @Override
  public void remove(T model) throws PersistException {
    Validate.notNull(model.getId(), "'id' is required");

    Node node = service().graph().getNodeById(model.getId());
    indexRemove(node);
    for(Relationship relationship: node.getRelationships()) {
      relationship.delete();
    }
    node.delete();
    logger.info("{} deleted", model);
  }

  public void removeRelationship(T model, String field, Model other) {
    RelatedTo relatedTo = Persistence.cache().fieldAnnotations(model.getClass(), field).relatedTo;
    Relationships.delete(model, relatedTo, other);
  }

  @Override
  public void save(T model) throws PersistException {
    save(new SaveContext<>(this, model));
  }

  @Override
  @SafeVarargs
  public final void save(T... entities) throws PersistException {
    for(T entity : entities) {
      save(entity);
    }
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    Persistence.save(this, context.model());
    logger.info("{} saved", context.model());
  }

  @Override
  public ResultIterator<T> search(String query) {
    return search(new SearchParameter().query(query));
  }

  @Override
  public ResultIterator<T> search(SearchParameter params) {
    if(!params.isFiltered() && !params.isOrdered() && params.returns() == null && params.page() == 1) {
      if(!CollectionUtils.isEmpty(params.ids())) {
        List<T> list = params.ids().stream()
          .map(id -> find(id, params.fields()))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
        return new ListIterator<>(this, list.iterator());
      } else if(!CollectionUtils.isEmpty(params.uuids())) {
        List<T> list = params.uuids().stream()
          .map(uuid -> findIndexedSingle(Model.Uuid, uuid, params))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
        return new ListIterator<>(this, list.iterator());
      }
    }
    
    Result result = queryBuilder().execute(params);
    return new ExecutionResultIterator<>(this, result, params);
  }

  @Override
  public <R extends Base> ResultIterator<R> searchRelated(SearchParameter params) {
    if(!params.returns().contains("e")) params.returns().add("e");
    Result result = queryBuilder().execute(params);
    if(params.returns() == null) {
      throw new UnsupportedOperationException("params.returns can't be null");
    }
    FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(getModelClass(), params.returns().get(0));
    if(descriptor.isRelationship()) {
      return new RelationshipResultIterator<>(result, params);
    } else {
      try {
        Repository repository = service().repository(descriptor.baseClass().getSimpleName());
        return new ExecutionResultIterator<>(repository, result, params);
      }
      catch(RepositoryInstantiationException e) {
        return new ExecutionResultIterator<>(service(), descriptor.baseClass(), result, params);
      }
    }
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


  @Override
  public Number sum(String field, SearchParameter params) {
    return queryBuilder().sum(field, params);
  }

  public void validateModel(SaveContext<T> context) {
    context.changedFields().forEach(f -> {
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
