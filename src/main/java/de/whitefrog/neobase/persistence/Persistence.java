package de.whitefrog.neobase.persistence;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.exception.DuplicateEntryException;
import de.whitefrog.neobase.exception.MissingRequiredException;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.annotation.RelationshipCount;
import de.whitefrog.neobase.model.relationship.BaseRelationship;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.repository.BaseModelRepository;
import de.whitefrog.neobase.repository.ModelRepository;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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
  public static <T extends Model> void delete(ModelRepository<T> repository, T model) {
    Node node = getNode(model);
    // check fields for bulk or indexed
    List<FieldDescriptor> fieldMap = cache.fieldMap(model.getClass());
    for(FieldDescriptor descriptor : fieldMap) {
      AnnotationDescriptor annotations = descriptor.annotations();
      if(annotations.indexed != null || annotations.unique) {
        // delete node from index
        repository.indexRemove(node);
//      } else if(annotations.relatedTo != null) {
//        for(Relationship relationship : node.getRelationships(
//          annotations.relatedTo.direction(), RelationshipType.withName(annotations.relatedTo.type()))) {
//          relationship.delete();
//        }
      }
    }
    for(org.neo4j.graphdb.Relationship relationship: node.getRelationships()) {
      relationship.delete();
    }

    // delete node
    node.delete();
  }

  /**
   * Save the specified model.
   *
   * @param repository Repository to saveField the model in.
   * @param context      The model to saveField.
   * @return The saved model.
   * @throws MissingRequiredException A required field is missing.
   */
  public static <T extends Model> T save(ModelRepository<T> repository, SaveContext<T> context) throws MissingRequiredException {
    T model = context.model();
    Label label = repository.label();
    boolean create = false;

    if(!model.isPersisted()) {
      create = true;
      Node node = service.graph().createNode(label);
      context.setNode(node);
      // add labels defined in repository
      repository.labels().stream()
        .filter(l -> !node.hasLabel(l))
        .forEach(node::addLabel);
      model.setId(node.getId());
      model.setCreated(System.currentTimeMillis());
      model.setType(label.name());
    } else {
      if(model.getType() == null) model.setType(label.name());
    }

    model.updateLastModified();

    // clone all properties from model
    for(FieldDescriptor field : context.fieldMap()) {
      saveField(context, field, create);
    }
    model.getCheckedFields().clear();

    logger.info("{} {}", model, create? "created": "updated");

    return model;
  }

  static <T extends Base> void saveField(SaveContext<T> context, FieldDescriptor descriptor, boolean created) {
    Field field = descriptor.field();
    AnnotationDescriptor annotations = descriptor.annotations();
    T model = context.model();
    PropertyContainer node = context.node();
    Repository<T> repository = context.repository();
    
    if(node == null) {
      throw new NullPointerException("node can not be null");
    }
    if(field == null) {
      throw new NullPointerException("field can not be null");
    }
    
    try {
      Object value = field.get(model);
      
      // when the annotation @Required is present, a value is expected
      if(created && annotations.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
        throw new MissingRequiredException(model, field);
      }

      boolean valueChanged = created || context.fieldChanged(field.getName());

      if(!annotations.notPersistant && !annotations.blob) {
        // Generate an uuid when the value is actually null
        if(created && annotations.uuid && field.get(model) == null) {
          String uuid = generateUuid();
          field.set(model, uuid);
          value = uuid;
          valueChanged = true;
        }

        if(value != null) {
          // handle relationships
          if(annotations.relatedTo != null && valueChanged && Model.class.isAssignableFrom(context.model().getClass())) {
            Relationships.saveField((SaveContext<? extends Model>) context, descriptor);
            logger.info("{}: updated relationships for \"{}\"", model, field.getName());
          }
          
          // check uniqueness
          else if(valueChanged && annotations.unique && !model.getCheckedFields().contains(field.getName())) {
            Optional<T> exist = context.repository().findIndexed(field.getName(), value).findAny();
            if(exist.isPresent() && model.getId() != exist.get().getId()) {
              throw new DuplicateEntryException(
                "A " + model.getClass().getSimpleName().toLowerCase() + " with the " +
                  field.getName() + " \"" + value + "\" already exists", model, field);
            } else if(exist.isPresent()) {
              logger.info("found already persisted {}, but seems to be equal to {}", exist, model);
            }
          }

          // Handle other values
          if(!(value instanceof Collection) && !(value instanceof Model)) {
            
            // store enum names
            if(value.getClass().isEnum()) {
              if((!node.hasProperty(field.getName()) || !((Enum<?>) value).name().equals(node.getProperty(field.getName())))) {
                node.setProperty(field.getName(), ((Enum<?>) value).name());
                logger.info("{}: set enum value for \"{}\" to \"{}\"", model, field.getName(), ((Enum<?>) value).name());
              }
            }
            // store dates as timestamp
            else if(value instanceof Date) {
              node.setProperty(field.getName(), ((Date) value).getTime());
              logger.info("{}: set date value for \"{}\" to \"{}\"", model, field.getName(), ((Date) value).getTime());
            }
            // store all other values
            else if(valueChanged) {
              node.setProperty(field.getName(), value);
              logger.info("{}: set value for \"{}\" to \"{}\"", model, field.getName(), value);
            }
          }
          // if the value has changed and the field is indexed, we need to add the value to the index
          if(valueChanged && (annotations.indexed != null || annotations.unique)) {
            if(!created) repository.indexRemove(node, field.getName());
            repository.index(model, field.getName(),
              value instanceof String? ((String) value).toLowerCase(): value);
          }
        } 
        // if the new value is null and @NullRemove is set on the field,
        // we need to remove the property from the node and the index
        else if(valueChanged && annotations.nullRemove) {
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
    BaseModelRepository<T> repository = service.repository(type);
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
      if(clazz == null) {
        // choose basic classes when there is none defined
        clazz = node instanceof Node? (Class<T>) Entity.class: (Class<T>) BaseRelationship.class;
      }
      T model = clazz.newInstance();
      model.setId(node instanceof Node? ((Node)node).getId(): ((org.neo4j.graphdb.Relationship) node).getId());
      fetch(model, fields, false);
      return model;
    } catch(IllegalStateException e) {
      throw e;
    } catch(Exception e) {
      throw e instanceof PersistException? (PersistException) e: new PersistException(e);
    }
  }

  private static Class getClass(PropertyContainer node) throws ClassNotFoundException {
    String className;
    if(node instanceof org.neo4j.graphdb.Relationship) {
      className = ((org.neo4j.graphdb.Relationship) node).getType().name();
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
  private static String generateUuid() {
    TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();
    UUID uuid = uuidGenerator.generate();
    return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
  }
  
  public static void removeProperty(Model model, String property) {
    Node node = getNode(model);
    node.removeProperty(property);
    try {
      Field field = model.getClass().getDeclaredField(property);
      if(!field.isAccessible()) field.setAccessible(true);
      field.set(model, null);
    } catch(ReflectiveOperationException e) {
      throw new NeobaseRuntimeException("field " + property + " could not be found on " + model, e);
    }
  }

  public static Node getNode(Model model) {
    Validate.notNull(model);
    Validate.notNull(model.getId(), "ID can not be null.");
    return service.graph().getNodeById(model.getId());
  }

  public static <T extends Base> void fetch(T model, String... fields) {
    fetch(model, FieldList.parseFields(Arrays.asList(fields)), false);
  }
  public static <T extends Base> void fetch(T model, FieldList fields) {
    fetch(model, fields, false);
  }
  public static <T extends Base> void fetch(T model, FieldList fields, boolean refetch) {
    if(model.getId() < 0) return;
    PropertyContainer node;

    try {
      if(model instanceof Relationship) {
        BaseRelationship relModel = (BaseRelationship) model;
        node = Relationships.getRelationship(relModel);
        org.neo4j.graphdb.Relationship relationship = Relationships.getRelationship(relModel);
        if((fields.containsField("from") || fields.containsField(Base.AllFields)) && (relModel.getFrom() == null || refetch)) {
          ((Relationship) model).setFrom(createRepositoryModel(
            relationship.getStartNode(), fields.containsField("from")? fields.get("from").subFields(): new FieldList()
          ));
        }
        if((fields.containsField("to") || fields.containsField(Base.AllFields)) && (relModel.getTo() == null || refetch)) {
          ((Relationship) model).setTo(createRepositoryModel(
            relationship.getEndNode(), fields.containsField("to")? fields.get("to").subFields(): new FieldList()
          ));
        }
      } else {
        node = Persistence.getNode((de.whitefrog.neobase.model.Model) model);
      }
      
      List<String> ignoredFields = Arrays.asList("id", "from", "to");
      for(FieldDescriptor descriptor: cache.fieldMap(model.getClass())) {
        if(CollectionUtils.isEmpty(fields) && descriptor.annotations().notPersistant) continue;
        if(ignoredFields.contains(descriptor.field().getName())) continue;
        if(descriptor.annotations().relatedTo != null) {
          boolean fetch = descriptor.annotations().fetch ||
            fields.containsField(Base.AllFields) || fields.containsField(descriptor.field().getName());
          if(!fetch) continue;
        }
        fetchField(node, model, descriptor, fields, refetch);
      }
    } catch(ReflectiveOperationException e) {
      logger.error("could not load relations for {}: {}", model, e.getMessage(), e);
    }
  }

  private static <T extends Base> void fetchField(PropertyContainer node, T model, FieldDescriptor descriptor,
                                                  FieldList fields, boolean refetch) throws ReflectiveOperationException {
    if(!refetch && model.getFetchedFields().contains(descriptor.field().getName())) return;
    AnnotationDescriptor annotations = descriptor.annotations();
    java.lang.reflect.Field field = descriptor.field();
    if(!field.getName().equals(Base.Type) && !annotations.fetch && !field.getName().equals(Base.Uuid) &&
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
          related = Relationships.getRelatedModels((Model) model, descriptor, fieldDescriptor, subFields);
        } else {
          related = Relationships.getRelationships((Model) model, descriptor, fieldDescriptor, subFields);
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
