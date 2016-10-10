package de.whitefrog.neobase.persistence;

import de.whitefrog.neobase.exception.MissingRequiredException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.exception.RelatedNotPersistedException;
import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import de.whitefrog.neobase.model.relationship.BaseRelationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.repository.Repository;
import de.whitefrog.neobase.Service;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public class Relationships {
  private static final Logger logger = LoggerFactory.getLogger(Relationships.class);
  private static Service service;
  private static ModelCache cache;
  
  public static void setService(Service _service) {
    service = _service;
  }
  public static void setCache(ModelCache _cache) {
    cache = _cache;
  }

  /**
   * Tests if a particular node has a specific relationship to another one.
   *
   * @param node  The node from where the relationship should start.
   * @param other The node, which should be related.
   * @param type  The relationship type.
   * @return true if a relationship exists, otherwise false.
   */
  public static boolean hasRelationshipTo(Node node, Node other, RelationshipType type) {
    return hasRelationshipTo(node, other, type, Direction.OUTGOING);
  }
  public static boolean hasRelationshipTo(Node node, Node other, RelationshipType type, Direction direction) {
    Iterable<Relationship> relationships = node.getRelationships(direction, type);
    for(Relationship relationship : relationships) {
      if(relationship.getEndNode().equals(other)) return true;
    }
    return false;
  }

  public static Relationship getRelationshipBetween(Model model, Model other, RelationshipType type) {
    return getRelationshipBetween(Persistence.getNode(model), Persistence.getNode(other), type);
  }

  /**
   * Get a relationship between two nodes.
   *
   * @param node  First node
   * @param other Second node
   * @param type  Relationship type
   * @return The found relationship, if none was found, null is returned.
   */
  public static Relationship getRelationshipBetween(Node node, Node other, RelationshipType type) {
    Iterable<Relationship> relationships = node.getRelationships(Direction.BOTH, type);
    for(Relationship relationship : relationships) {
      if(relationship.getEndNode().equals(other)) return relationship;
    }
    return null;
  }

  public static Relationship getRelationshipBetween(Node node, Node other, RelationshipType type, Direction dir) {
    Iterable<Relationship> relationships = node.getRelationships(dir, type);
    for(Relationship relationship : relationships) {
      if(relationship.getEndNode().equals(other)) return relationship;
    }
    return null;
  }
  
  public static <R extends de.whitefrog.neobase.model.relationship.Relationship<? extends Model, ? extends Model>> R get(
      Node node, Node other, RelationshipType type, Direction dir) throws ReflectiveOperationException {
    Relationship relationship = getRelationshipBetween(node, other, type, dir);
    String className = (String) relationship.getProperty(Base.Type);
    if(!cache.containsModel(className)) {
      throw new ClassNotFoundException(className);
    }
    Class<R> clazz = (Class<R>) cache.getModel(className);
    R model = clazz.newInstance();
    fetchFields(model, relationship, node, new FieldList());
    return model;
  }

  public static Model getRelatedModel(Model model, RelatedTo annotation, FieldList fields) throws ReflectiveOperationException {
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    try {
      Relationship relationship = Persistence.getNode(model).getSingleRelationship(
        RelationshipType.withName(annotation.type()), annotation.direction());
      if(relationship != null) {
        Node node = Persistence.getNode(model);
        Node other = relationship.getOtherNode(node);
        String type = (String) other.getProperty(Base.Type);
        Repository<Model> repository = service.repository(type);
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

  public static <M extends de.whitefrog.neobase.model.Model> Set<M> getRelatedModels(Model model, FieldDescriptor descriptor, 
      QueryField fieldDescriptor, FieldList fields) throws ReflectiveOperationException {
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
      String type = (String) other.getProperty(Base.Type);
      if(annotation.restrictType() && !type.equals(descriptor.baseClass().getSimpleName())) {
        count--; continue;
      }
      Repository<M> repository = service.repository(type);
      models.add(repository.createModel(other, fields));
    }
    iterator.close();
    return models;
  }

  public static <R extends BaseRelationship> Set<R> getRelationships(Model model, FieldDescriptor descriptor, 
      QueryField fieldDescriptor, FieldList fields) throws ReflectiveOperationException {
    RelatedTo annotation = descriptor.annotations().relatedTo;
    Validate.notNull(model);
    Validate.notNull(annotation.type());

    ResourceIterator<Relationship> iterator =
      (ResourceIterator<Relationship>) Persistence.getNode(model).getRelationships(
        annotation.direction(), RelationshipType.withName(annotation.type())).iterator();

    Node node = Persistence.getNode(model);
    Set<R> models = new HashSet<>();
    long count = 0;
    while(iterator.hasNext()) {
      if(fieldDescriptor != null && count < fieldDescriptor.skip()) { iterator.next(); count++; continue; }
      if(fieldDescriptor != null && count++ == fieldDescriptor.skip() + fieldDescriptor.limit()) break;
      
      Relationship relationship = iterator.next();
      R newRel = (R) descriptor.baseClass().newInstance();
      fetchFields(newRel, relationship, model, node, fields);
      
      models.add(newRel);
    }
    iterator.close();
    return models;
  }
  
  public static void fetch(de.whitefrog.neobase.model.relationship.Relationship<? extends Model, ? extends Model> relModel) {
    fetch(relModel, new FieldList());
  }
  
  public static void fetch(de.whitefrog.neobase.model.relationship.Relationship<? extends Model, ? extends Model> relModel,
                           FieldList fields) {
    Relationship relationship = getRelationshipBetween(
      Persistence.getNode(relModel.getFrom()), Persistence.getNode(relModel.getTo()), 
      RelationshipType.withName(relModel.getClass().getSimpleName()));
    List<String> bypass = Arrays.asList("id", "from", "to");
    
    try {
      for(FieldDescriptor descriptor: cache.fieldMap(relModel.getClass())) {
        if(bypass.contains(descriptor.field().getName())) continue;
        Persistence.fetchField(relationship, relModel, descriptor, fields, false);
      }
    } catch(ReflectiveOperationException e) {
      logger.error("could not load relations for {}: {}", relModel, e.getMessage(), e);
    }
  }

  private static void fetchFields(de.whitefrog.neobase.model.relationship.Relationship relModel,
                                  Relationship relationship, Node node, FieldList fields) throws ReflectiveOperationException {
    fetchFields(relModel, relationship, Persistence.get(node), node, fields);
  }
  private static void fetchFields(de.whitefrog.neobase.model.relationship.Relationship relModel,
                                  Relationship relationship, Model model, Node node, FieldList fields) throws ReflectiveOperationException {
    Node other = relationship.getOtherNode(node);
    boolean isStart = relationship.getStartNode().equals(node);
    String type = (String) other.getProperty(Base.Type);
    Repository<? extends Model> repository = service.repository(type);
    if(repository == null) throw new NullPointerException("no repository with type: " + type);
    relModel.setId(relationship.getId());
    relModel.setType(relationship.getType().name());
    Persistence.fetch(model, FieldList.parseFields(Base.Type), false);
    if(isStart) {
      FieldList fieldList = fields.containsField("from")? fields.get("from").subFields(): new FieldList();
      Model from = service.repository(model.getType()).createModel(node, fieldList);
      fieldList = fields.containsField("to")? fields.get("to").subFields(): new FieldList();
      Model to = repository.createModel(other, fieldList);
      relModel.setFrom(from);
      relModel.setTo(to);
    } else {
      FieldList fieldList = fields.containsField("from")? fields.get("from").subFields(): new FieldList();
      Model from = repository.createModel(other, fieldList);
      fieldList = fields.containsField("to")? fields.get("to").subFields(): new FieldList();
      Model to = service.repository(model.getType()).createModel(node, fieldList);
      relModel.setFrom(from);
      relModel.setTo(to);
    }
    
    List<String> bypass = Arrays.asList("id", "from", "to");
    for(FieldDescriptor descriptor: cache.fieldMap(relModel.getClass())) {
      if(bypass.contains(descriptor.field().getName())) continue;
      Persistence.fetchField(relationship, relModel, descriptor, fields, false);
    }
  }

  static <T extends de.whitefrog.neobase.model.Model> void save(T model, Node node, FieldDescriptor descriptor, AnnotationDescriptor annotations)
      throws IllegalAccessException {
    RelatedTo relatedTo = annotations.relatedTo;
    Object value = descriptor.field().get(model);
    if(!(value instanceof Collection)) {
      Relationship existing = node.getSingleRelationship(
        RelationshipType.withName(relatedTo.type()), relatedTo.direction());
      if(existing != null) {
        existing.delete();
      }

      Model foreignModel = (Model) value;
      addRelationship(model, node, relatedTo, foreignModel);
    } else {
      Collection collection = (Collection) value;
      if(!annotations.lazy) {
        // check if relationship is obsolete and delete if neccessary
        RelationshipType relationshipType = RelationshipType.withName(relatedTo.type());
        for(Relationship relationship : node.getRelationships(relatedTo.direction(), relationshipType)) {
          Base other = Persistence.get(relationship.getOtherNode(node));
          if(!collection.contains(other)) {
            relationship.delete();
            logger.info("relationship between {} and {} removed", model, other);
          }
        }
      }
      if(descriptor.isModel()) {
        // add the relationship if the foreign model is persisted
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
        for(de.whitefrog.neobase.model.relationship.Relationship<Model, Model> relModel :
          ((Collection<de.whitefrog.neobase.model.relationship.Relationship>) value)) {
          if(!relModel.getTo().isPersisted()) {
            throw new RelatedNotPersistedException(
              "the " + relModel.getTo() + " (" + relatedTo.type() + ") is not yet persisted");
          }

          Model other = null;
          if(relatedTo.direction().equals(Direction.INCOMING)) {
            other = relModel.getFrom();
            if(!relModel.getTo().equals(model)) {
              throw new PersistException(relModel + " should have " + model + " as 'to' field set");
            }
          }
          if(relatedTo.direction().equals(Direction.OUTGOING)) {
            other = relModel.getTo();
            if(!relModel.getFrom().equals(model)) {
              throw new PersistException(relModel + " should have " + model + " as 'from' field set");
            }
          }
          if(relatedTo.direction().equals(Direction.BOTH)) {
            // TODO: Handle "BOTH"
            throw new UnsupportedOperationException("BOTH is not supported yet");
          }
          Relationship relationship;
          if(relModel.getId() <= 0) {
            relationship = addRelationship(model, node, relatedTo, other);
            if(relationship != null) {
              relModel.setId(relationship.getId());
              relModel.setCreated(System.currentTimeMillis());
              saveFields(relationship, relModel, true);
            }
          } else {
            relationship = service.graph().getRelationshipById(relModel.getId());
            relModel.updateLastModified();
            saveFields(relationship, relModel, false);
          }
        }
      }
    }
  }
  
  public static void save(de.whitefrog.neobase.model.relationship.Relationship<? extends Model, ? extends Model> model) {
    Relationship relationship;
    if(model.getId() <= 0) {
      Node fromNode = Persistence.getNode(model.getFrom());
      Node toNode = Persistence.getNode(model.getTo());
      RelationshipType type = RelationshipType.withName(model.getClass().getSimpleName());
      relationship = getRelationshipBetween(fromNode, toNode, type, Direction.OUTGOING);
      if(relationship == null) {
        relationship = fromNode.createRelationshipTo(toNode, type);
      }
      model.setId(relationship.getId());
      model.setCreated(System.currentTimeMillis());
    } else {
      relationship = service.graph().getRelationshipById(model.getId());
      model.updateLastModified();
    }
    saveFields(relationship, model, false);
  }

  private static void saveFields(Relationship relationship, de.whitefrog.neobase.model.relationship.Relationship<? extends Model, ? extends Model> model, boolean created) {
    for(FieldDescriptor descriptor: cache.fieldMap(model.getClass())) {
      AnnotationDescriptor annotations = descriptor.annotations();
      try {
        Object value = descriptor.field().get(model);
        // when the annotation Required is present, a value is expected
        if(created && annotations.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
          throw new MissingRequiredException(model, descriptor.field());
        }

        boolean valueChanged = (value != null && !relationship.hasProperty(descriptor.getName())) ||
          (relationship.hasProperty(descriptor.getName()) && !relationship.getProperty(descriptor.getName()).equals(value));

        if(!annotations.notPersistant && !annotations.blob) {
          if(annotations.uuid && descriptor.field().get(model) == null) {
            String uuid = Persistence.generateUuid();
            descriptor.field().set(model, uuid);
            value = uuid;
            valueChanged = true;
          }

          if(value != null) {
            if(!(value instanceof Collection) && !(value instanceof Model)) {
              if(value.getClass().isEnum()) {
                if((!relationship.hasProperty(descriptor.getName()) || !((Enum<?>) value).name().equals(relationship.getProperty(descriptor.getName())))) {
                  // store enum names
                  relationship.setProperty(descriptor.getName(), ((Enum<?>) value).name());
                }
              }  else if(value instanceof Date) {
                relationship.setProperty(descriptor.getName(), ((Date) value).getTime());
              } else if(valueChanged) {
                // store default values
                relationship.setProperty(descriptor.getName(), value);
              }
            }
          } else if(valueChanged && annotations.nullRemove) {
            relationship.removeProperty(descriptor.getName());
          }
        }
      } catch(ReflectiveOperationException e) {
        logger.error("Could not get property on {}: {}", model, e.getMessage(), e);
      } catch(IllegalArgumentException e) {
        logger.error("Could not store property {} on {}: {}", descriptor.getName(), model, e.getMessage());
      }
    }
  }

  public static <T extends de.whitefrog.neobase.model.Model> Relationship addRelationship(T model, Node node, RelatedTo annotation, Model foreignModel) {
    if(foreignModel.getId() == -1) {
      if(foreignModel.getUuid() != null) {
        foreignModel = service.repository(foreignModel.getClass()).findByUuid(foreignModel.getUuid());
      } else {
        throw new RelatedNotPersistedException(
          "the related field " + foreignModel + " is not yet persisted");
      }
    }
    Relationship relationship = null;
    Node foreignNode = Persistence.getNode(foreignModel);
    RelationshipType relationshipType = RelationshipType.withName(annotation.type());
    if(!annotation.multiple() && hasRelationshipTo(node, foreignNode, relationshipType, annotation.direction())) {
      return null;
    }
    if(annotation.direction() == Direction.OUTGOING) {
        relationship = node.createRelationshipTo(foreignNode, relationshipType);
        logger.info("created relationship {} ({}) from {} to {}", 
          annotation.type(), relationship.getId(), model, foreignModel);
    } else if(annotation.direction() == Direction.INCOMING) {
        relationship = foreignNode.createRelationshipTo(node, relationshipType);
        logger.info("created relationship {} ({}) from {} to {}", 
          annotation.type(), relationship.getId(), foreignModel, model);
    } else if(annotation.direction() == Direction.BOTH) {
      // TODO: implement sth useful here
    }
    
    return relationship;
  }

  public static <T extends de.whitefrog.neobase.model.Model> void remove(T model, RelationshipType type,
                                                                         Direction direction, Model foreignModel) {
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
      if(relationship.getEndNode().equals(foreignNode)) relationship.delete();
    }
  }
}
