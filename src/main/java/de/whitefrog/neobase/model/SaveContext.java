package de.whitefrog.neobase.model;

import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.persistence.AnnotationDescriptor;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.ModelCache;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.Node;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SaveContext<T extends de.whitefrog.neobase.model.Model> {
  private T model;
  private T original;
  private Repository<T> repository;
  private Node node;
  private Set<Field> changedFields;
  private List<FieldDescriptor> fieldMap;

  public SaveContext(Repository<T> repository, T model) {
    this.repository = repository;
    this.model = model;
    if(model.getId() > 0) {
      this.original = repository.createModel(node());
    }
    else if(model.getUuid() != null) {
      this.original = repository.findByUuid(model.getUuid());
    }
    fieldMap = Persistence.cache().fieldMap(model.getClass());
  }

  public Set<Field> changedFields() {
    if(changedFields == null) findChangedFields();
    return changedFields;
  }
  
  public boolean fieldChanged(String fieldName) {
    try {
      Field field = ModelCache.getField(model.getClass(), fieldName);
      return fieldChanged(field);
    } catch(NoSuchFieldException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
  }
  private boolean fieldChanged(Field field) {
    AnnotationDescriptor annotation = 
      Persistence.cache().fieldAnnotations(repository().getModelClass(), field.getName());
    try {
      if(!field.isAccessible()) field.setAccessible(true);
      Object value = field.get(model);
      if(value != null && !annotation.nullRemove) {
        if(original == null) {
          return true;
        }
        else {
          if(annotation.relatedTo != null && annotation.lazy) return true;
          repository().fetch(original, FieldList.parseFields(field.getName()+"(max)"));
          Object originalValue = field.get(original);
          return !value.equals(originalValue);
        }
      }
    } catch(IllegalAccessException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
    
    return false;
  }

  private void findChangedFields() {
    changedFields = fieldMap.stream()
      .map(FieldDescriptor::field)
      .filter(this::fieldChanged)
      .collect(Collectors.toSet());
  }

  public T model() {
    return model;
  }

  public Repository<T> repository() {
    return repository;
  }

  public Node node() {
    if(node == null && model.getId() != -1) {
      node = repository.getNode(model);
    }
    return node;
  }

  public T original() {
    return original;
  }
}
