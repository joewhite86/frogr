package de.whitefrog.froggy.persistence;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.exception.PersistException;
import de.whitefrog.froggy.exception.RelatedNotPersistedException;
import de.whitefrog.froggy.exception.RepositoryNotFoundException;
import de.whitefrog.froggy.model.Base;
import de.whitefrog.froggy.model.Entity;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.SaveContext;
import de.whitefrog.froggy.model.annotation.RelatedTo;
import de.whitefrog.froggy.model.relationship.BaseRelationship;
import de.whitefrog.froggy.model.rest.FieldList;
import de.whitefrog.froggy.model.rest.QueryField;
import de.whitefrog.froggy.repository.DefaultRelationshipRepository;
import de.whitefrog.froggy.repository.ModelRepository;
import de.whitefrog.froggy.repository.RelationshipRepository;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles most persistent operations required for neo4j to deal with froggy relationships.
 * Some relationship operations are kept in the Persistence class.
 */
public class Relationships {
  private static final Logger logger = LoggerFactory.getLogger(Relationships.class);
  private static Service service;

  public static void setService(Service _service) {
    service = _service;
  }

  /**
   * Adds a relationship to a existing node. Tests for multiple relationships and a persisted foreign node. 
   */
  private static <T extends Model> BaseRelationship addRelationship(
        T model, Node node, RelatedTo annotation, Model foreignModel) {
    if(foreignModel.getId() == -1) {
      if(foreignModel.getUuid() != null) {
        foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
      } else {
        throw new RelatedNotPersistedException(
          "the related field " + foreignModel + " is not yet persisted");
      }
    }
    Node foreignNode = Persistence.getNode(foreignModel);
    RelationshipType relationshipType = RelationshipType.withName(annotation.type());
    if(!annotation.multiple() && hasRelationshipTo(node, foreignNode, relationshipType, annotation.direction())) {
      return Persistence.get(getRelationshipBetween(node, foreignNode, relationshipType, annotation.direction()));
//      throw new RelationshipExistsException("a relationship " + annotation.type() +
//        " between " + model + " and " + foreignModel + " already exists");
    }
    RelationshipRepository<BaseRelationship<Model, Model>> repository;
    try {
      repository = service.repository(relationshipType.name());
    } catch(RepositoryNotFoundException e) {
      // if no repository is found in the RepositoryFactory, create a new one here
      // TODO: this should be handled inside of RepositoryFactory but then it would  
      // TODO: create a DefaultRealtionshipRepository for each unknown type name passed 
      // TODO: and i haven't found a solution yet
      repository = new DefaultRelationshipRepository<>(service, relationshipType.name());
      service.repositoryFactory().register(relationshipType.name(), repository);
    }
    BaseRelationship<Model, Model> relationship;
    
    if(annotation.direction() == Direction.OUTGOING) {
      relationship = repository.createModel(model, foreignModel);
    } else if(annotation.direction() == Direction.INCOMING) {
      relationship = repository.createModel(foreignModel, model);
    } else {
      // TODO: implement sth useful here
      throw new IllegalArgumentException();
    }
    repository.save(relationship);
    
    return relationship;
  }

  public static <T extends BaseRelationship> T getRelationshipBetween(
        Model model, Model other, RelationshipType type, Direction dir) {
    return Persistence.get( 
      getRelationshipBetween(Persistence.getNode(model), Persistence.getNode(other), type, dir));
  }

  public static Relationship getRelationshipBetween(Node node, Node other, RelationshipType type, Direction dir) {
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
  static Model getRelatedModel(Model model, RelatedTo annotation, FieldList fields) {
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    try {
      Relationship relationship = Persistence.getNode(model).getSingleRelationship(
        RelationshipType.withName(annotation.type()), annotation.direction());
      if(relationship != null) {
        Node node = Persistence.getNode(model);
        Node other = relationship.getOtherNode(node);
        String type = (String) other.getProperty(Entity.Type);
        ModelRepository<Model> repository = service.repository(type);
        return repository.createModel(other, fields);
      }
    } catch(NotFoundException e) {
      if(e.getMessage().startsWith("More than")) {
        logger.error(e.getMessage());
        logger.error("Relationships are:");
        Persistence.getNode(model).getRelationships(
          RelationshipType.withName(annotation.type()), annotation.direction())
          .forEach(rel -> logger.error(rel.toString()));
        throw e;
      }
    }
    return null;
  }

  static <M extends Model> Set<M> getRelatedModels(Model model, FieldDescriptor descriptor,
                                                   QueryField fieldDescriptor, FieldList fields) {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) Persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    Set<M> models = new HashSet<>();
    Node node = Persistence.getNode(model);
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
  public static <R extends BaseRelationship> Relationship getRelationship(R relationship) {
    return service.graph().getRelationshipById(relationship.getId());
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
  static <R extends BaseRelationship> Set<R> getRelationships(Model model, FieldDescriptor descriptor,
                                                              QueryField queryField, FieldList fields) {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) Persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    Set<R> models = new HashSet<>();
    long count = 0;
    while(iterator.hasNext()) {
      if(queryField != null && count < queryField.skip()) { iterator.next(); count++; continue; }
      if(queryField != null && count++ == queryField.skip() + queryField.limit()) break;

      Relationship relationship = iterator.next();
      models.add(Persistence.get(relationship, fields));
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
  public static boolean hasRelationshipTo(Node node, Node other, RelationshipType type, Direction direction) {
    Iterable<Relationship> relationships = node.getRelationships(direction, type);
    for(Relationship relationship : relationships) {
      if(relationship.getOtherNode(node).equals(other)) return true;
    }
    return false;
  }

  /**
   * Save method called from RelationshipRepository's
   */
  public static <T extends BaseRelationship> T save(SaveContext<T> context) {
    T model = context.model();
    boolean create = false;

    if(!model.getPersisted()) {
      create = true;
      if(model.getFrom() == null ) {
        throw new IllegalArgumentException("cannot create relationship with no \"from\" field set");
      }
      if(model.getTo() == null ) {
        throw new IllegalArgumentException("cannot create relationship with no \"to\" field set");
      }
      Node fromNode = Persistence.getNode(model.getFrom());
      Node toNode = Persistence.getNode(model.getTo());
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

    // clone all properties from model
    for(FieldDescriptor field : context.fieldMap()) {
      Persistence.saveField(context, field, create);
    }
    model.getCheckedFields().clear();

    if(logger.isInfoEnabled()) {
      logger.info("Relationship {}({}, {}) {}", model.getType(), model.getFrom(), model.getTo(), 
        create? "created": "updated");
    }

    return model;
  }

  /**
   * Used only from Persistence class with the models save context and field descriptor
   */
  @SuppressWarnings("unchecked")
  static <T extends Model> void saveField(SaveContext<T> context, FieldDescriptor descriptor)
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

      Model foreignModel = (Model) value;
      addRelationship(model, node, relatedTo, foreignModel);
    } 
    // Handle collections
    else {
      Collection collection = (Collection) value;
      // check if relationship is obsolete and delete if neccessary
      if(!annotations.lazy) {
        RelationshipType relationshipType = RelationshipType.withName(relatedTo.type());
        for(Relationship relationship : node.getRelationships(relatedTo.direction(), relationshipType)) {
          Base other = Persistence.get(relationship.getOtherNode(node));
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
          if(foreignModel.getId() == -1) {
            if(foreignModel.getUuid() != null) {
              foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
            }
            else {
              throw new RelatedNotPersistedException(
                "the related field " + foreignModel + " (" + relatedTo.type() + ") is not yet persisted");
            }
          }
          addRelationship(model, node, relatedTo, foreignModel);
        }
      }
      // Handle collections of relationship models
      else {
        for(BaseRelationship<Model, Model> relModel: ((Collection<BaseRelationship>) value)) {
          if(!relModel.getTo().getPersisted()) {
            throw new RelatedNotPersistedException(
              "the " + relModel.getTo() + " (" + relatedTo.type() + ") is not yet persisted");
          }
          
          RelationshipRepository<BaseRelationship> repository = 
            (RelationshipRepository<BaseRelationship>) service.repository(relModel.getClass());

          if(relatedTo.direction().equals(Direction.INCOMING)) {
            if(!relModel.getTo().equals(model)) {
              throw new PersistException(relModel + " should have " + model + " as 'to' field set");
            }
          } else if(relatedTo.direction().equals(Direction.OUTGOING)) {
            if(!relModel.getFrom().equals(model)) {
              throw new PersistException(relModel + " should have " + model + " as 'from' field set");
            }
          } else if(relatedTo.direction().equals(Direction.BOTH)) {
            // TODO: Handle "BOTH"
            throw new UnsupportedOperationException("BOTH is not supported yet");
          }
          repository.save(relModel);
        }
      }
    }
  }

  /**
   * Used in BaseRelationshipRepository to delete an entire relationship
   * @param relationship Relationship model to delete
   */
  public static <R extends BaseRelationship> void delete(R relationship) {
    getRelationship(relationship).delete();
    logger.info("relationship {} between {} and {} removed", 
      relationship.type(), relationship.getFrom(), relationship.getTo());
  }

  public static <T extends Model> void delete(T model, RelationshipType type, Direction direction, Model foreignModel) {
    if(foreignModel.getId() == -1) {
      if(foreignModel.getUuid() != null) {
        foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
      } else {
        throw new RelatedNotPersistedException(
          "the related field " + foreignModel + " is not yet persisted");
      }
    }
    Node node = Persistence.getNode(model);
    Node foreignNode = Persistence.getNode(foreignModel);
    for(Relationship relationship: node.getRelationships(type, direction)) {
      if(relationship.getOtherNode(node).equals(foreignNode)) {
        relationship.delete();
        logger.info("relationship {} between {} and {} removed", 
          type.name(), model, foreignModel);
      }
    }
  }
}
