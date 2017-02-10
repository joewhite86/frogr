package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.DefaultResultIterator;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.helper.Streams;
import de.whitefrog.neobase.index.IndexUtils;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.relationship.BaseRelationship;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public abstract class BaseRelationshipRepository<T extends BaseRelationship> 
    extends BaseRepository<T> implements RelationshipRepository<T> {
  private final Logger logger;

  public BaseRelationshipRepository(Service service) {
    super(service);
    this.logger = LoggerFactory.getLogger(getClass());
  }
  public BaseRelationshipRepository(Service service, String modelName) {
    super(service, modelName);
    this.logger = LoggerFactory.getLogger(getClass());
  }
  
  @Override
  public T createModel(PropertyContainer node, FieldList fields) {
    return fetch(Persistence.get(node), false, fields);
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
  public Stream<T> findIndexed(Index index, String field, Object value) {
    return findIndexed(index, field, value, new SearchParameter());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Stream<T> findIndexed(Index index, String field, Object value, SearchParameter params) {
    IndexHits<org.neo4j.graphdb.Relationship> found = IndexUtils.query(index, field,
      value instanceof String? ((String) value).toLowerCase(): value.toString(),
      params.limit() * params.page());
    return Streams.get(new DefaultResultIterator<>(this, found, params));
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
  public void remove(T model) throws PersistException {
    Validate.notNull(model.getId(), "'id' is required");
    Relationships.delete(model);
    logger.info("{} deleted", model);
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    // TODO: Fix
    Relationships.save(context);
    logger.info("{} saved", context.model());
  }
}
