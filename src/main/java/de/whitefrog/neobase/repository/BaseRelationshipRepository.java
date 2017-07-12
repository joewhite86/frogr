package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.relationship.BaseRelationship;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

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
  public T createModel(Model from, Model to) {
    try {
      Constructor constructor = ConstructorUtils.getMatchingAccessibleConstructor(getModelClass(),
        new Class[] {from.getClass(), to.getClass()});
      return (T) constructor.newInstance(from, to);
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
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
    boolean create = !context.model().getPersisted();
    Relationships.save(context);
    logger().info("{} {}", context.model(), create? "created": "updated");
  }
}
