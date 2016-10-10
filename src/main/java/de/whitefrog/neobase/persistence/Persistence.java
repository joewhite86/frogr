package de.whitefrog.neobase.persistence;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import de.whitefrog.neobase.exception.MissingRequiredException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.annotation.RelationshipCount;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.repository.Repository;
import de.whitefrog.neobase.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class Persistence {
  private static final Logger logger = LoggerFactory.getLogger(Persistence.class);
  private static Service service;
  private static ModelCache cache;

  /**
   * Set the service to use.
   *
   * @param _service Service to use.
   */
  public static void setService(Service _service) {
    service = _service;
    cache = new ModelCache(_service.registry());
    Relationships.setService(_service);
    Relationships.setCache(cache);
  }

  public static ModelCache cache() {
    return cache;
  }

  /**
   * Delete a model from repository.
   *
   * @param repository The repository to delete from.
   * @param model      The model to delete.
   */
  public static <T extends de.whitefrog.neobase.model.Model> void delete(Repository<T> repository, T model) {
    Node node = getNode(model);
    // check fields for bulk or indexed
    List<FieldDescriptor> fieldMap = cache.fieldMap(model.getClass());
    for(FieldDescriptor descriptor : fieldMap) {
      AnnotationDescriptor annotations = descriptor.annotations();
      if(annotations.indexed != null || annotations.unique) {
        // remove node from index
        repository.indexRemove(node);
      } else if(annotations.relatedTo != null) {
        for(Relationship relationship : node.getRelationships(
          annotations.relatedTo.direction(), RelationshipType.withName(annotations.relatedTo.type()))) {
          relationship.delete();
        }
      }
    }

    // delete node
    node.delete();
  }

  /**
   * Save the specified model.
   *
   * @param repository Repository to save the model in.
   * @param model      The model to save.
   * @return The saved model.
   * @throws MissingRequiredException A required field is missing.
   */
  public static <T extends Model> T save(Repository<T> repository, T model) throws MissingRequiredException {
    Validate.notNull(model);
    Node node;
    Label label = repository.label();
    boolean create = false;

    if(model.getId() == -1 && model.getUuid() == null) {
      create = true;
      node = service.graph().createNode(label);
      // add labels defined in repository
      repository.labels().stream()
        .filter(l -> !node.hasLabel(l))
        .forEach(node::addLabel);
      model.setId(node.getId());
      model.setCreated(System.currentTimeMillis());
      model.setType(label.name());
    } else if(model.getId() > 0) {
      if(model.getType() == null) model.setType(label.name());
      node = service.graph().getNodeById(model.getId());
    } else {
      if(model.getType() == null) model.setType(label.name());
      node = getNode(repository.findByUuid(model.getUuid()));
    }

    model.updateLastModified();

    if(!node.hasLabel(repository.label())) {
      node.addLabel(repository.label());
      repository.labels().stream()
        .filter(l -> !node.hasLabel(l))
        .forEach(node::addLabel);
    }

    // clone all properties from model
    for(FieldDescriptor field : cache.fieldMap(model.getClass())) {
      saveField(repository, model, node, field, create);
    }
    model.getCheckedFields().clear();

    logger.info("{} {}", model, create? "created": "saved");

    return model;
  }

  private static <T extends Model> void saveField(Repository<T> repository, T model, Node node,
                                                  FieldDescriptor descriptor,
                                                  boolean created) {
    java.lang.reflect.Field field = descriptor.field();
    AnnotationDescriptor annotations = descriptor.annotations();
    try {
      Object value = field.get(model);
      
      // when the annotation @Required is present, a value is expected
      if(created && annotations.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
        throw new MissingRequiredException(model, field);
      }

      boolean valueChanged = created || value == null || !node.hasProperty(field.getName()) ||
        (node.hasProperty(field.getName()) && !node.getProperty(field.getName()).equals(value));

      if(!annotations.notPersistant && !annotations.blob) {
        if(created && annotations.uuid && field.get(model) == null) {
          String uuid = generateUuid();
          field.set(model, uuid);
          value = uuid;
          valueChanged = true;
        }

        if(value != null) {
          if(annotations.relatedTo != null) {
            // handle relationships
            Relationships.save(model, node, descriptor, annotations);
          } else if(valueChanged && annotations.unique && !model.getCheckedFields().contains(field.getName())) {
            // check uniqueness
            T exist = repository.findIndexedSingle(field.getName(), value);
            if(exist != null && model.getId() != exist.getId()) {
              throw new ConstraintViolationException(
                "failed to save " + model + ", a " + model.getClass().getSimpleName() + " with " +
                  field.getName() + " \"" + value + "\" already exists: " + exist);
            } else if(exist != null) {
              logger.info("found already persisted {}, but seems to be equal to {}", exist, model);
            }
          }

          if(!(value instanceof Collection) && !(value instanceof Model)) {
            if(value.getClass().isEnum()) {
              if((!node.hasProperty(field.getName()) || !((Enum<?>) value).name().equals(node.getProperty(field.getName())))) {
                // store enum names
                node.setProperty(field.getName(), ((Enum<?>) value).name());
              }
            } else if(value instanceof Date) {
              node.setProperty(field.getName(), ((Date) value).getTime());
            } else if(valueChanged) {
              // store default values
              node.setProperty(field.getName(), value);
            }
          }
          if(valueChanged && (annotations.indexed != null || annotations.unique)) {
            if(!created) repository.indexRemove(node, field.getName());
            repository.index(model, field.getName(),
              value instanceof String? ((String) value).toLowerCase(): value);
          }
        } else if(valueChanged && annotations.nullRemove) {
          node.removeProperty(field.getName());
          if((annotations.indexed != null || annotations.unique)) {
            repository.indexRemove(node, field.getName());
          }
        }
      }
    } catch(ReflectiveOperationException e) {
      logger.error("Could not get property on {}: {}", model, e.getMessage(), e);
    } catch(IllegalArgumentException e) {
      logger.error("Could not store property {} on {}: {}", field.getName(), model, e.getMessage());
    }
  }
  
  private static <T extends Model> T createRepositoryModel(Node node, FieldList fields) {
    String type = (String) node.getProperty(Base.Type);
    Repository<T> repository = service.repository(type);
    return repository.createModel(node, fields);
  }

  /**
   * Get a model instance from a neo node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  public static <T extends Base> T get(PropertyContainer node) throws PersistException {
    return get(node, new FieldList());
  }

  /**
   * Get a model instance from a neo node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  @SuppressWarnings("unchecked")
  public static <T extends Base> T get(PropertyContainer node, FieldList fields) throws PersistException {
    try {
      Class<T> clazz = (Class<T>) getClass(node);
      return get(node, clazz, fields);
    } catch(IllegalStateException e) {
      throw e;
    } catch(Exception e) {
      throw e instanceof PersistException? (PersistException) e: new PersistException(e);
    }
  }

  private static <T extends Base> T get(PropertyContainer node, Class<T> clazz, FieldList fields) {
    T model;
    try {
      model = clazz.newInstance();
      model.setId(node instanceof Node? ((Node)node).getId(): ((Relationship) node).getId());
      if(node instanceof Relationship && model instanceof de.whitefrog.neobase.model.relationship.Relationship) {
        ((de.whitefrog.neobase.model.relationship.Relationship) model).setFrom(createRepositoryModel(
          ((Relationship) node).getStartNode(), fields.containsField("from")? fields.get("from").subFields(): new FieldList()
        ));
        ((de.whitefrog.neobase.model.relationship.Relationship) model).setTo(createRepositoryModel(
          ((Relationship) node).getEndNode(), fields.containsField("to")? fields.get("to").subFields(): new FieldList()
        ));
      }

      for(FieldDescriptor descriptor: cache.fieldMap(clazz)) {
        if(CollectionUtils.isEmpty(fields) && descriptor.annotations().notPersistant) continue;
        if(descriptor.annotations().relatedTo != null) {
          boolean fetch = descriptor.annotations().fetch || 
            fields.containsField(Base.AllFields) || fields.containsField(descriptor.field().getName());
          if(!fetch) continue;
        }
        fetchField(node, model, descriptor, fields, false);
      }
    } catch(Exception e) {
      throw e instanceof PersistException? (PersistException) e: new PersistException(e);
    }

    return model;
  }

  private static Class getClass(PropertyContainer node) throws ClassNotFoundException {
    String className;
    if(node instanceof Relationship) {
      className = ((Relationship) node).getType().name();
    } else {
      className = (String) node.getProperty(
        node.hasProperty(Entity.Model)? Entity.Model: Base.Type);
    }
    return cache().getModel(className);
  }

  /**
   * Generate a fresh uuid.
   *
   * @return Generated uuid.
   */
  public static String generateUuid() {
    TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();
    UUID uuid = uuidGenerator.generate();
    return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
  }

  public static Node getNode(Model model) {
    Validate.notNull(model);
    Validate.notNull(model.getId(), "ID can not be null.");
    return service.graph().getNodeById(model.getId());
  }

  public static <T extends Model> void fetch(T model, FieldList fields, boolean refetch) {
    if(model.getId() <= 0) return;

    try {
      Node node = getNode(model);
      for(FieldDescriptor descriptor: cache.fieldMap(model.getClass())) {
        fetchField(node, model, descriptor, fields, refetch);
      }
    } catch(ReflectiveOperationException e) {
      logger.error("could not load relations for {}: {}", model, e.getMessage(), e);
    }
  }

  static <T extends Base> void fetchField(PropertyContainer node, T model, FieldDescriptor descriptor,
                                          FieldList fields, boolean refetch) throws ReflectiveOperationException {
    if(!refetch && model.getFetchedFields().contains(descriptor.field().getName())) return;
    AnnotationDescriptor annotations = descriptor.annotations();
    java.lang.reflect.Field field = descriptor.field();
    if(!field.getName().equals(Base.Type) && !annotations.fetch && 
      !fields.containsField(Base.AllFields) && !fields.containsField(field.getName())) return;
    field.setAccessible(true);
    
    // fetch relationship count only
    if(node instanceof Node && annotations.relationshipCount != null && fields.containsField(field.getName())) {
      RelationshipCount count = annotations.relationshipCount;
      field.set(model, Iterables.count(((Node) node).getRelationships(count.direction(), RelationshipType.withName(count.type()))));
    }
    
    // fetch related nodes
    else if(model instanceof Model && annotations.relatedTo != null) {
      if(!annotations.fetch && !fields.containsField(field.getName())) return;
      FieldList subFields = fields.containsField(field.getName())? 
        fields.get(field.getName()).subFields(): new FieldList();
      QueryField fieldDescriptor = fields.containsField(field.getName())? 
        fields.get(field.getName()): new QueryField(field.getName());
      // ignore relationships when only AllFields is set
      if(descriptor.isCollection()) {
        Collection related;
        if(descriptor.isModel()) {
          related = Relationships.getRelatedModels((Model) model, field, annotations.relatedTo,
            fieldDescriptor, subFields);
        } else {
          related = Relationships.getRelationships((Model) model, field, annotations.relatedTo,
            fieldDescriptor, subFields);
        }
        field.set(model, Set.class.isAssignableFrom(field.getType())? related: new ArrayList<>(related));        
      } else {
        Model related = Relationships.getRelatedModel((Model) model, annotations.relatedTo, subFields);
        field.set(model, related);
      }
    } 
    
    // fetch normal field values
    else if(node.hasProperty(field.getName())) {
      if(Enum.class.isAssignableFrom(field.getType())) {
        field.set(model, Enum.valueOf((Class<Enum>) field.getType(),
          (String) node.getProperty(field.getName())));
      } else if(Date.class.isAssignableFrom(field.getType())) {
        field.set(model, new Date((Long) node.getProperty(field.getName())));
      } else {
        field.set(model, node.getProperty(field.getName()));
      }
    }
    model.getFetchedFields().add(field.getName());
  }
  
  public static <T extends Model> T findByUuid(String label, String uuid) {
    ResourceIterator<? extends PropertyContainer> iterator = 
      service.graph().findNodes(Label.label(label), Model.Uuid, uuid);
    return iterator.hasNext()? get(iterator.next()): null;
  }
}
