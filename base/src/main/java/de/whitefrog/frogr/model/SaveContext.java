package de.whitefrog.frogr.model;

import de.whitefrog.frogr.exception.FrogrException;
import de.whitefrog.frogr.persistence.AnnotationDescriptor;
import de.whitefrog.frogr.persistence.FieldDescriptor;
import de.whitefrog.frogr.persistence.Persistence;
import de.whitefrog.frogr.repository.Repository;
import org.neo4j.graphdb.PropertyContainer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Context for entity/relationship save operations.
 * Takes one entity or relationship and tests for property and relationship changes.
 * @param <T> Entity type
 */
public class SaveContext<T extends Base> {
  /**
   * Model used for this save operation.
   */
  private T model;
  /**
   * The original fetched from database.
   */
  private T original;
  /**
   * The repository used in the save context.
   */
  private Repository<T> repository;
  /**
   * The neo4j node or relationship.
   */
  private PropertyContainer node;
  /**
   * List of FieldDescriptor's containing fields that 
   * changed compared to the original.
   */
  private List<FieldDescriptor> changedFields;
  /**
   * Reference to the model's field map.
   */
  private List<FieldDescriptor> fieldMap;

  public SaveContext(Repository<T> repository, T model) {
    this.repository = repository;
    this.model = model;
    if(model.getId() > -1) {
      original = repository.createModel(node());
    }
    else if(model.getUuid() != null) {
      original = repository.findByUuid(model.getUuid());
      model.setId(original.getId());
    }
    fieldMap = Persistence.cache().fieldMap(model.getClass());
  }

  /**
   * Get a full list of changed fields.
   * @return List of changed fields
   */
  public List<FieldDescriptor> changedFields() {
    if(changedFields == null) {
      if(original() != null) repository.fetch(original(), Entity.AllFields);
      changedFields = fieldMap.stream()
        .filter(f-> fieldChanged(f.field()))
        .collect(Collectors.toList());
    }
    return changedFields;
  }

  /**
   * Test if a single field has changed.
   * @param fieldName Field name.
   * @return True if the field has changed
   */
  public boolean fieldChanged(String fieldName) {
    return changedFields().stream().anyMatch(f -> f.getName().equals(fieldName));
  }
  private boolean fieldChanged(Field field) {
    AnnotationDescriptor annotation = 
      Persistence.cache().fieldAnnotations(repository().getModelClass(), field.getName());
    try {
      if(!field.isAccessible()) field.setAccessible(true);
      Object value = field.get(model);
      if(value != null && !annotation.nullRemove) {
        if(original() == null) {
          return true;
        }
        else {
          if(annotation.relatedTo != null && annotation.lazy) return true;
          if(annotation.relatedTo != null) repository().fetch(original(), FieldList.parseFields(field.getName()+"(max)"));
          Object originalValue = field.get(original());
          return !value.equals(originalValue);
        }
      } else if(annotation.nullRemove) {
        return true;
      }
    } catch(IllegalAccessException e) {
      throw new FrogrException(e.getMessage(), e);
    }
    
    return false;
  }

  /**
   * Get the field map for the model in this context.
   * @return Field map of the model in this context.
   */
  public List<FieldDescriptor> fieldMap() {
    return fieldMap;
  }

  /**
   * Get the current model.
   * @return The model in this context.
   */
  public T model() {
    return model;
  }

  /**
   * Get the model repository.
   * @return The model repository.
   */
  @SuppressWarnings("unchecked")
  public Repository<T> repository() {
    return repository;
  }

  /**
   * Get the neo4j node or relationship used as reference in this context.
   * @return The neo4j node or relationship used as reference in this context.
   */
  @SuppressWarnings("unchecked")
  public <N extends PropertyContainer> N node() {
    if(node == null && original() != null) {
      if(model instanceof Model) node = repository.graph().getNodeById(original().getId());
      else node = repository.graph().getRelationshipById(original().getId());
    } else if(node == null && model().getId() > -1) {
      if(model instanceof Model) node = repository.graph().getNodeById(model().getId());
      else node = repository.graph().getRelationshipById(model().getId());
    }
    return (N) node;
  }

  /**
   * The original model used as reference in this context.
   * @return The original model used as reference in this context.
   */
  public T original() {
    return original;
  }

  /**
   * Set the neo4j node. Used when a new node is created.
   * !!! Should not be used outside of persistence methods.
   * @param node The neo4j node
   */
  public void setNode(PropertyContainer node) {
    this.node = node;
  }
}
