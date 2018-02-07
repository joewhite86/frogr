package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.exception.FrogrException;
import de.whitefrog.frogr.exception.PersistException;
import de.whitefrog.frogr.model.FieldList;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.relationship.BaseRelationship;
import de.whitefrog.frogr.model.relationship.Relationship;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Base repository for relationships.
 * Provides some basic functionality like model creation and persistance.
 */
public abstract class BaseRelationshipRepository<T extends BaseRelationship> 
    extends BaseRepository<T> implements RelationshipRepository<T> {
  private final Logger logger;

  public BaseRelationshipRepository() {
    super();
    this.logger = LoggerFactory.getLogger(getClass());
  }
  public BaseRelationshipRepository(String modelName) {
    super(modelName);
    this.logger = LoggerFactory.getLogger(getClass());
  }

  @Override
  public T createModel(Model from, Model to) {
    try {
      Constructor constructor = ConstructorUtils.getMatchingAccessibleConstructor(getModelClass(),
        new Class[] {from.getClass(), to.getClass()});
      return (T) constructor.newInstance(from, to);
    } catch(ReflectiveOperationException e) {
      throw new FrogrException(e.getMessage(), e);
    }
  }

  @Override
  public T createModel(PropertyContainer node, FieldList fields) {
    return service().persistence().get(node, fields);
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
    service().persistence().relationships().delete(model);
    logger.info("{} deleted", model);
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    validateModel(context);
    boolean create = !context.model().getPersisted();
    service().persistence().relationships().save(context);
    logger().info("{} {}", context.model(), create? "created": "updated");
  }
}
