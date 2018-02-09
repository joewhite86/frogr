package de.whitefrog.frogr.persistence;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.exception.*;
import de.whitefrog.frogr.model.*;
import de.whitefrog.frogr.model.Entity;
import de.whitefrog.frogr.model.annotation.RelatedTo;
import de.whitefrog.frogr.model.relationship.BaseRelationship;
import de.whitefrog.frogr.repository.DefaultRelationshipRepository;
import de.whitefrog.frogr.repository.ModelRepository;
import de.whitefrog.frogr.repository.RelationshipRepository;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles most persistent operations required for neo4j to deal with frogr relationships.
 * Some relationship operations are kept in the persistence class.
 */
public class Relationships {
  private static final Logger logger = LoggerFactory.getLogger(Relationships.class);
  private Service service;
  private Persistence persistence;

  Relationships(Service service, Persistence persistence) {
    this.service = service;
    this.persistence = persistence;
  }

  /**
   * Adds a relationship to a existing node. Tests for multiple relationships and a persisted foreign node. 
   */
  private <T extends Model> void addRelationship(
        T model, Node node, RelatedTo annotation, Model foreignModel) {
    if(foreignModel.getId() == -1) {
      if(foreignModel.getUuid() != null) {
        foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
      } else {
        throw new RelatedNotPersistedException(
          "the related field " + foreignModel + " is not yet persisted");
      }
    }
    Node foreignNode = persistence.getNode(foreignModel);
    RelationshipType relationshipType = RelationshipType.withName(annotation.type());
    if(!annotation.multiple() && hasRelationshipTo(node, foreignNode, relationshipType, annotation.direction())) {
      // the relationship already exists, no more work to do
      return;
    }
    RelationshipRepository<BaseRelationship<Model, Model>> repository;
    try {
      repository = service.repository(relationshipType.name());
    } catch(RepositoryNotFoundException e) {
      // if no repository is found in the RepositoryFactory, create a new one here
      // TODO: this should be handled inside of RepositoryFactory but then it would  
      // TODO: create a DefaultRealtionshipRepository for each unknown type name passed 
      // TODO: and i haven't found a solution yet
      repository = new DefaultRelationshipRepository<>(relationshipType.name());
      try {
        service.repositoryFactory().setRepositoryService(repository);
        service.repositoryFactory().register(relationshipType.name(), repository);
      } catch(ReflectiveOperationException ex) {
        throw new RepositoryInstantiationException(ex.getCause());
      }
    }
    BaseRelationship<Model, Model> relationship;
    
    if(annotation.direction() == Direction.OUTGOING) {
      relationship = repository.createModel(model, foreignModel);
    } else if(annotation.direction() == Direction.INCOMING) {
      relationship = repository.createModel(foreignModel, model);
    } else {
      // on Direction.BOTH we can create either a OUTGOING or an INCOMING relation
      relationship = repository.createModel(model, foreignModel);
    }
    repository.save(relationship);
  }

  public <T extends BaseRelationship> T getRelationshipBetween(
        Model model, Model other, RelationshipType type, Direction dir) {
    Relationship relationship = 
      getRelationshipBetween(persistence.getNode(model), persistence.getNode(other), type, dir);
    return relationship == null? null: persistence.get(relationship);
  }

  public Relationship getRelationshipBetween(Node node, Node other, RelationshipType type, Direction dir) {
    Iterable<Relationship> relationships = node.getRelationships(dir, type);
    for(Relationship relationship : relationships) {
      if(relationship.getOtherNode(node).equals(other)) return relationship;
    }
    return null;
  }

  /**
   * Get a single related model
   * @param model Model that contains the relationship
   * @param annotation RelatedTo annotation
   * @param fields Fields that should get fetched for the related model
   * @return The related model or null when none exists
   */
  Model getRelatedModel(Model model, RelatedTo annotation, FieldList fields) {
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    try {
      Relationship relationship = persistence.getNode(model).getSingleRelationship(
        RelationshipType.withName(annotation.type()), annotation.direction());
      if(relationship != null) {
        Node node = persistence.getNode(model);
        Node other = relationship.getOtherNode(node);
        String type = (String) other.getProperty(Entity.Type);
        ModelRepository<Model> repository = service.repository(type);
        return repository.createModel(other, fields);
      }
    } catch(NotFoundException e) {
      if(e.getMessage().startsWith("More than")) {
        logger.error(e.getMessage());
        logger.error("Relationships are:");
        persistence.getNode(model).getRelationships(
          RelationshipType.withName(annotation.type()), annotation.direction())
          .forEach(rel -> logger.error(rel.toString()));
        throw e;
      }
    }
    return null;
  }

  <M extends Model> Set<M> getRelatedModels(Model model, FieldDescriptor descriptor,
                                                   QueryField fieldDescriptor, FieldList fields) {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    Set<M> models = new HashSet<>();
    Node node = persistence.getNode(model);
    long count = 0;

    while(iterator.hasNext()) {
      if(count < fieldDescriptor.skip()) { iterator.next(); count++; continue; }
      if(count++ == fieldDescriptor.skip() + fieldDescriptor.limit()) break;

      Relationship relationship = iterator.next();
      Node other = relationship.getOtherNode(node);
      String type = (String) other.getProperty(Entity.Type);
      if(annotation.restrictType() && !type.equals(descriptor.baseClass().getSimpleName())) {
        count--; continue;
      }
      ModelRepository<M> repository = service.repository(type);
      models.add(repository.createModel(other, fields));
    }
    iterator.close();
    return models;
  }

  /**
   * Get the neo4j relationship from a model. A id has to be set.
   * @param relationship The relationship model
   * @return The corresponding neo4j relationship
   */
  public <R extends de.whitefrog.frogr.model.relationship.Relationship> Relationship getRelationship(R relationship) {
    if(relationship.getId() > -1) {
      return service.graph().getRelationshipById(relationship.getId());
    }
    else {
      throw new UnsupportedOperationException("cant find relationship without id");
    }
  }

  /**
   * Get all relationships matching a model's field descriptor. 
   * For all relationships fetch the fields passed.
   *
   * @param model Model that contains the relationships
   * @param descriptor Descriptor for the relationship field
   * @param fields Field list to fetch for the relationships
   * @return Set of matching relationships
   * descriptor could not be created
   */
  @SuppressWarnings("unchecked")
  <R extends BaseRelationship> R getRelationship(
      Model model, FieldDescriptor descriptor, FieldList fields) {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    R relationshipModel = null;
    if(iterator.hasNext()) {
      Relationship relationship = iterator.next();
      relationshipModel = persistence.get(relationship, fields);
    }
    iterator.close();
    return relationshipModel;
  }

  /**
   * Get all relationships matching a model's field descriptor. 
   * For all relationships fetch the fields passed.
   * 
   * @param model Model that contains the relationships
   * @param descriptor Descriptor for the relationship field
   * @param queryField QueryField used for paging
   * @param fields Field list to fetch for the relationships
   * @return Set of matching relationships
   * descriptor could not be created
   */
  @SuppressWarnings("unchecked")
  <R extends BaseRelationship> Set<R> getRelationships(Model model, FieldDescriptor descriptor,
                                                              QueryField queryField, FieldList fields) {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    Set<R> models = new HashSet<>();
    long count = 0;
    while(iterator.hasNext()) {
      if(queryField != null && count < queryField.skip()) { iterator.next(); count++; continue; }
      if(queryField != null && count++ == queryField.skip() + queryField.limit()) break;

      Relationship relationship = iterator.next();
      models.add(persistence.get(relationship, fields));
    }
    iterator.close();
    return models;
  }

  /**
   * Tests if a particular node has a specific relationship to another one.
   *
   * @param node  The node from where the relationship should start.
   * @param other The node, which should be related.
   * @param type  The relationship type.
   * @return true if a relationship exists, otherwise false.
   */
  public boolean hasRelationshipTo(Node node, Node other, RelationshipType type, Direction direction) {
    Iterable<Relationship> relationships = node.getRelationships(direction, type);
    for(Relationship relationship : relationships) {
      if(relationship.getOtherNode(node).equals(other)) return true;
    }
    return false;
  }

  /**
   * Save method called from RelationshipRepository's
   */
  public <T extends de.whitefrog.frogr.model.relationship.Relationship> T save(SaveContext<T> context) {
    T model = context.model();
    boolean create = false;

    if(!model.getPersisted()) {
      create = true;
      
      if(!model.getFrom().getPersisted()) 
        throw new FrogrException("the model " + model.getFrom() + " is not yet persisted, but used as 'from' in relationship " + model);
      if(!model.getTo().getPersisted())
        throw new FrogrException("the model " + model.getTo() + " is not yet persisted, but used as 'to' in relationship " + model);
      
      Node fromNode = persistence.getNode(model.getFrom());
      Node toNode = persistence.getNode(model.getTo());
      RelationshipType relType = RelationshipType.withName(context.repository().getType());
      Relationship relationship = fromNode.createRelationshipTo(toNode, relType);
      context.setNode(relationship);
      model.setId(relationship.getId());
      model.setCreated(System.currentTimeMillis());
      model.setType(model.getClass().getSimpleName());
    } else {
      if(model.getType() == null) model.setType(context.repository().getType());
      model.updateLastModified();
    }

    for(String property: model.getRemoveProperties()) {
      removeProperty(model, property);
    }
    // clone all properties from model
    for(FieldDescriptor field : context.fieldMap()) {
      persistence.saveField(context, field, create);
    }
    model.getCheckedFields().clear();

    if(logger.isInfoEnabled()) {
      logger.info("Relationship {}({}, {}) {}", model.getType(), model.getFrom(), model.getTo(), 
        create? "created": "updated");
    }

    return model;
  }
  
  @SuppressWarnings("unchecked")
  private <T extends BaseRelationship> void save(Model model, T relModel, RelatedTo annotation) {
    if(!relModel.getFrom().getPersisted()) {
      throw new RelatedNotPersistedException(
        "the 'from' model " + relModel.getFrom() + " (" + annotation.type() + ") is not yet persisted");
    }
    if(!relModel.getTo().getPersisted()) {
      throw new RelatedNotPersistedException(
        "the 'to' model " + relModel.getTo() + " (" + annotation.type() + ") is not yet persisted");
    }

    RelationshipRepository<BaseRelationship> repository =
      (RelationshipRepository<BaseRelationship>) service.repository(relModel.getClass());

    if(annotation.direction().equals(Direction.INCOMING)) {
      if(!relModel.getTo().equals(model)) {
        throw new PersistException(relModel + " should have " + model + " as 'to' field set");
      }
    } else if(annotation.direction().equals(Direction.OUTGOING)) {
      if(!relModel.getFrom().equals(model)) {
        throw new PersistException(relModel + " should have " + model + " as 'from' field set");
      }
    } else if(annotation.direction().equals(Direction.BOTH)) {
      if(!relModel.getFrom().equals(model) && !relModel.getTo().equals(model)) {
        throw new PersistException(relModel + "should have " + model + " either set as 'from' or 'to' field");
      }
    }
    repository.save(relModel);
  }

  /**
   * Used only from persistence class with the models save context and field descriptor.
   */
  @SuppressWarnings("unchecked")
  <T extends Model> void saveField(SaveContext<T> context, FieldDescriptor descriptor)
      throws IllegalAccessException {
    AnnotationDescriptor annotations = descriptor.annotations();
    T model = context.model();
    Node node = context.node();
    RelatedTo relatedTo = annotations.relatedTo;
    Object value = descriptor.field().get(model);
    // Handle single relationships
    if(!descriptor.isCollection()) {
      Relationship existing = node.getSingleRelationship(
        RelationshipType.withName(relatedTo.type()), relatedTo.direction());
      if(existing != null) {
        existing.delete();
      }

      if(descriptor.isModel()) {
        Model foreignModel = (Model) value;
        addRelationship(model, node, relatedTo, foreignModel);
      } else {
        BaseRelationship relModel = (BaseRelationship) value;
        save(model, relModel, relatedTo);
      }
    } 
    // Handle collections
    else {
      Collection collection = (Collection) value;
      // check if relationship is obsolete and delete if neccessary
      if(!annotations.lazy) {
        RelationshipType relationshipType = RelationshipType.withName(relatedTo.type());
        for(Relationship relationship : node.getRelationships(relatedTo.direction(), relationshipType)) {
          Base other = persistence.get(relationship.getOtherNode(node));
          if(!collection.contains(other)) {
            relationship.delete();
            logger.info("relationship between {} and {} removed", model, other);
          }
        }
      }
      // Handle collection of models
      // add the relationship if the foreign model is persisted
      if(descriptor.isModel()) {
        for(Model foreignModel : ((Collection<Model>) value)) {
          addRelationship(model, node, relatedTo, foreignModel);
        }
      }
      // Handle collections of relationship models
      else {
        for(BaseRelationship<Model, Model> relModel: ((Collection<BaseRelationship>) value)) {
          save(model, relModel, relatedTo);
        }
      }
    }
  }

  /**
   * Removes a property inside the graph and on the model.
   *
   * @param model Model to remove the property from
   * @param property Property name to remove
   */
  public void removeProperty(de.whitefrog.frogr.model.relationship.Relationship model, String property) {
    Relationship node = getRelationship(model);
    node.removeProperty(property);
    try {
      Field field = model.getClass().getDeclaredField(property);
      if(!field.isAccessible()) field.setAccessible(true);
      field.set(model, null);
    } catch(ReflectiveOperationException e) {
      throw new FrogrException("field " + property + " could not be found on " + model, e);
    }
  }

  /**
   * Used in BaseRelationshipRepository to delete an entire relationship
   * @param relationship Relationship model to delete
   */
  public <R extends BaseRelationship> void delete(R relationship) {
    getRelationship(relationship).delete();
    logger.info("relationship {} between {} and {} removed", 
      relationship.type(), relationship.getFrom(), relationship.getTo());
  }

  public <T extends Model> void delete(T model, RelationshipType type, Direction direction, Model foreignModel) {
    if(foreignModel.getId() == -1) {
      if(foreignModel.getUuid() != null) {
        foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
      } else {
        throw new RelatedNotPersistedException(
          "the related field " + foreignModel + " is not yet persisted");
      }
    }
    Node node = persistence.getNode(model);
    Node foreignNode = persistence.getNode(foreignModel);
    for(Relationship relationship: node.getRelationships(type, direction)) {
      if(relationship.getOtherNode(node).equals(foreignNode)) {
        relationship.delete();
        logger.info("relationship {} between {} and {} removed", 
          type.name(), model, foreignModel);
      }
    }
  }
}
