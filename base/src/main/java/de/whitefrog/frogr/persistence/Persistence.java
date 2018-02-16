package de.whitefrog.frogr.persistence;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.exception.*;
import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.*;
import de.whitefrog.frogr.model.Entity;
import de.whitefrog.frogr.model.annotation.IndexType;
import de.whitefrog.frogr.model.annotation.RelationshipCount;
import de.whitefrog.frogr.model.relationship.BaseRelationship;
import de.whitefrog.frogr.model.relationship.Relationship;
import de.whitefrog.frogr.repository.ModelRepository;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * The link between the models and repositories and neo4j itself.
 * Handles every persitent operation required for neo4j to deal with the models and relationships
 * used in frogr. For Relationships the operations are moved to the Relationships class but 
 * some operations common to nodes and relationships are kept here.
 */
@SuppressWarnings("unchecked")
public class Persistence {
  private static final Logger logger = LoggerFactory.getLogger(Persistence.class);
  private Service service;
  private ModelCache cache;
  private Relationships relationships;
  private TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();
  
  public Persistence(Service service, ModelCache cache) {
    this.service = service;
    this.cache = cache;
    relationships = new Relationships(service, this);
  }

  public ModelCache cache() {
    return cache;
  }

  public Relationships relationships() {
    return relationships;
  }

  /**
   * Delete a model from repository.
   *
   * @param model      The model to delete.
   */
  public <T extends Model> void delete(T model) {
    Node node = getNode(model);
    
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
  public <T extends Model> T save(ModelRepository<T> repository, SaveContext<T> context) throws MissingRequiredException {
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
      model.updateLastModified();
    }

    for(String property: context.model().getRemoveProperties()) {
      removeProperty(context.model(), property);
    }
    // clone all properties from model
    for(FieldDescriptor field : context.fieldMap()) {
      saveField(context, field, create);
    }
    model.getCheckedFields().clear();

    logger.info("{} {}", model, create? "created": "updated");

    return model;
  }

  @SuppressWarnings("unchecked")
  <T extends Base> void saveField(SaveContext<T> context, FieldDescriptor descriptor, boolean created) {
    Field field = descriptor.field();
    AnnotationDescriptor annotations = descriptor.annotations();
    T model = context.model();
    PropertyContainer node = context.node();
    
    Object value = null;
    try {
      value = field.get(model);
      
      // when the annotation @Required is present, a value is expected
      if(created && annotations.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
        throw new MissingRequiredException(model, field);
      }

      boolean valueChanged = created || context.fieldChanged(field.getName());

      if(!annotations.notPersistent && !annotations.blob) {
        // Generate an uuid when the value is actually null
        if(created && annotations.uuid && field.get(model) == null) {
          String uuid = generateUuid();
          field.set(model, uuid);
          value = uuid;
          valueChanged = true;
        }

        if(value != null) {
          // handle relationships
          if(annotations.relatedTo != null && valueChanged && model instanceof Model) {
            relationships.saveField((SaveContext<? extends Model>) context, descriptor);
          }
          // Handle other values
          else if(!(value instanceof Collection) && !(value instanceof Model)) {
            // store enum names
            if(value.getClass().isEnum()) {
              if((!node.hasProperty(field.getName()) || !((Enum<?>) value).name().equals(node.getProperty(field.getName())))) {
                node.setProperty(field.getName(), ((Enum<?>) value).name());
                logger.info("{}: set enum value \"{}\" to \"{}\"", model, field.getName(), ((Enum<?>) value).name());
              }
            }
            // store dates as timestamp
            else if(value instanceof Date) {
              node.setProperty(field.getName(), ((Date) value).getTime());
              logger.info("{}: set date value \"{}\" to \"{}\"", model, field.getName(), ((Date) value).getTime());
            }
            // store all other values
            else if(valueChanged) {
              node.setProperty(field.getName(), value);
              logger.info("{}: set value \"{}\" to \"{}\"", model, field.getName(), value);
              if(value instanceof String && annotations.indexed != null && annotations.indexed.type().equals(IndexType.LowerCase)) {
                node.setProperty(field.getName() + "_lower", ((String) value).toLowerCase());
                logger.info("{}: set value \"{}\" to \"{}\"", model, field.getName() + "_lower", ((String) value).toLowerCase());
              }
            }
          }
        } 
        // if the new value is null and @NullRemove is set on the field,
        // we need to remove the property from the node and the index
        else if(valueChanged && annotations.nullRemove) {
          logger.info("{}: removed value \"{}\"", model, field.getName());
          node.removeProperty(field.getName());
          if(annotations.indexed != null && annotations.indexed.type().equals(IndexType.LowerCase)) {
            node.removeProperty(field.getName() + "_lower");
          }
        }
      }
    } catch(ReflectiveOperationException e) {
      logger.error("Could not get property on {}: {}", model, e.getMessage(), e);
    } catch(ConstraintViolationException e) {
      throw new DuplicateEntryException("A " + model.getClass().getSimpleName().toLowerCase() + " with the " +
        field.getName() + " \"" + value + "\" already exists", model, field);
    } catch(IllegalArgumentException e) {
      logger.error("Could not store property {} on {}: {}", field.getName(), model, e.getMessage());
    }
  }

  /**
   * Get a model instance from a neo node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  public <T extends Base> T get(PropertyContainer node) throws PersistException {
    return get(node, new FieldList());
  }

  /**
   * Get a model instance from a neo4j node.
   *
   * @param node Node to create the model from
   * @return The created model
   * @throws PersistException Is thrown if a field can not be converted
   */
  @SuppressWarnings("unchecked")
  public <T extends Base> T get(PropertyContainer node, FieldList fields) throws PersistException {
    Validate.notNull(node, "node can't be null");
    try {
      Class<T> clazz = (Class<T>) getClass(node);
      if(clazz == null) {
        // choose basic classes when there is none defined
        clazz = node instanceof Node? (Class<T>) Model.class: (Class<T>) BaseRelationship.class;
      }
      T model;
      if(node instanceof Node) {
        model = clazz.newInstance();
        model.setId(((Node) node).getId());
      } else {
        org.neo4j.graphdb.Relationship rel = (org.neo4j.graphdb.Relationship) node; 
        Model from = get(rel.getStartNode(), fields.getOrEmpty("from").subFields());
        Model to = get(rel.getEndNode(), fields.getOrEmpty("to").subFields());
        
        Constructor<T> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, new Class[] {from.getClass(), to.getClass()});
        model = constructor.newInstance(from, to);
        model.setId(rel.getId());
        fields = new FieldList(fields);
        fields.remove(new QueryField("from"));
        fields.remove(new QueryField("to"));
      }
      service.repository(clazz).fetch(model, fields);
      return model;
    } catch(IllegalStateException e) {
      throw e;
    } catch(Exception e) {
      throw e instanceof PersistException? (PersistException) e: new PersistException(e);
    }
  }

  /**
   * Get the model class for a node or relationship.
   * The "model" property will be preferred, else "type" will be taken for evaluation.
   * 
   * @param node Node or relationship to get the corresponding class for
   * @return The correct model class for the node or relationship passed
   */
  private Class getClass(PropertyContainer node) {
    String className;
    if(node instanceof org.neo4j.graphdb.Relationship) {
      className = ((org.neo4j.graphdb.Relationship) node).getType().name();
    } else {
      className = (String) node.getProperty(
        node.hasProperty(Model.Model)? Entity.Model: Entity.Type);
    }
    return cache().getModel(className);
  }

  /**
   * Generate a fresh uuid.
   *
   * @return Generated uuid.
   */
  private String generateUuid() {
    UUID uuid = uuidGenerator.generate();
    return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
  }

  /**
   * Removes a property inside the graph and on the model.
   * 
   * @param model Model to remove the property from
   * @param property Property name to remove
   */
  private void removeProperty(Model model, String property) {
    FieldDescriptor descriptor = cache().fieldDescriptor(model.getClass(), property);
    if(descriptor == null) throw new FieldNotFoundException(property, model);
    
    Node node = getNode(model);
    node.removeProperty(property);
    if(descriptor.annotations().indexed != null && descriptor.annotations().indexed.type().equals(IndexType.LowerCase)) {
      node.removeProperty(property + "_lower");
    }
    try {
      Field field = ReflectionUtil.getSuperField(model.getClass(), property);
      if(!field.isAccessible()) field.setAccessible(true);
      field.set(model, null);
    } catch(ReflectiveOperationException e) {
      throw new FieldNotFoundException(property, model);
    }
  }

  /**
   * Get the neo4j node for a particular model using its id.
   * 
   * @param model Model to search inside the graph
   * @return The neo4j node equivalent for the passed model
   */
  public Node getNode(Model model) {
    Validate.notNull(model);
    if(model.getId() > -1) {
      return service.graph().getNodeById(model.getId());
    } else if(model.getUuid() != null && model.getType() != null) {
      Node node = service.graph().findNode(Label.label(model.getType()), Entity.Uuid, model.getUuid());
      model.setId(node.getId());
      return node;
    } else {
      throw new UnsupportedOperationException("cant get a node without id or uuid");
    }
  }

  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   * 
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch
   */
  public <T extends Base> void fetch(T model, String... fields) {
    fetch(model, FieldList.parseFields(Arrays.asList(fields)), false);
  }
  
  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   *
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch as FieldList
   */
  public <T extends Base> void fetch(T model, FieldList fields) {
    fetch(model, fields, false);
  }
  
  /**
   * Fetch properties for the passed model from database.
   * This can be either normal properties or relationships if annotated correctly.
   *
   * @param model The model to fetch the properties for
   * @param fields List of fields to fetch as FieldList
   * @param refetch Fetch even if the field was already fetched before
   */
  public <T extends Base> void fetch(T model, FieldList fields, boolean refetch) {
    Validate.notNull(model, "model cannot be null");
    if(!model.isPersisted()) throw new FrogrException("the model " + model + " is not persisted yet");
    PropertyContainer node;

    try {
      if(model instanceof Relationship) {
        BaseRelationship relModel = (BaseRelationship) model;
        node = relationships.getRelationship(relModel);
      } else {
        node = getNode((Model) model);
      }
      
      // iterate over fields to ensure fields with @Fetch annotation and 'allFields' will be fetched
      for(FieldDescriptor descriptor : cache.fieldMap(model.getClass())) {
        // don't fetch the 'id' field and fields annotated with @NotPersistent
        if(descriptor.getName().equals("id") && descriptor.annotations().notPersistent) continue;

        // always fetch when field is 'type' or 'uuid', or when allFields is set 
        // or the field list contains the current field
        boolean fetch = descriptor.annotations().fetch ||
          descriptor.getName().equals(Entity.Type) || descriptor.getName().equals(Entity.Uuid) ||
          fields.containsField(Entity.AllFields) || fields.containsField(descriptor.getName());

        if(fetch) {
          // still fetch if refetch is true or field is not in fetchedFields 
          // or fields does not contiain field or sub-fields is not empty 
          if(!refetch && model.getFetchedFields().contains(descriptor.getName()) &&
            fields.containsField(descriptor.getName()) && fields.get(descriptor.getName()).subFields().isEmpty())
            continue;

          fetchField(node, model, descriptor, fields);
        }
      }
    } catch(ReflectiveOperationException e) {
      logger.error("could not load relations for {}: {}", model, e.getMessage(), e);
    }
  }

  private <T extends Base> void fetchField(PropertyContainer node, T model, FieldDescriptor descriptor,
                                                  FieldList fields) throws ReflectiveOperationException {
    AnnotationDescriptor annotations = descriptor.annotations();
    Field field = descriptor.field();
    field.setAccessible(true);

    if(node instanceof org.neo4j.graphdb.Relationship) {
      Relationship relModel = (Relationship) model;
      if(field.getName().equals("from") && fields.containsField("from") && !fields.get("from").subFields().isEmpty()) {
        service.repository(relModel.getFrom().getClass()).fetch(relModel.getFrom(), fields.get("from").subFields());
      }
      if(field.getName().equals("to") && fields.containsField("to") && !fields.get("to").subFields().isEmpty()) {
        service.repository(relModel.getTo().getClass()).fetch(relModel.getTo(), fields.get("to").subFields());
      }
    }
    
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
          related = relationships.getRelatedModels((Model) model, descriptor, fieldDescriptor, subFields);
        } else {
          related = relationships.getRelationships((Model) model, descriptor, fieldDescriptor, subFields);
        }
        field.set(model, Set.class.isAssignableFrom(field.getType())? related: new ArrayList<>(related));        
      } else {
        Base related;
        if(descriptor.isModel()) {
          related = relationships.getRelatedModel((Model) model, annotations.relatedTo, subFields);
        } else {
          related = relationships.getRelationship((Model) model, descriptor, subFields);
        }
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

  /**
   * Get a model by label and uuid.
   * 
   * @param label The label/type to look for
   * @param uuid The uuid for the model
   * @return The found model or null if none was found
   */
  public <T extends Model> T findByUuid(String label, String uuid) {
    ResourceIterator<? extends PropertyContainer> iterator = 
      service.graph().findNodes(Label.label(label), Entity.Uuid, uuid);
    return iterator.hasNext()? get(iterator.next()): null;
  }
}
