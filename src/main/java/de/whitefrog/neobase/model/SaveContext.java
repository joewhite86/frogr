package de.whitefrog.neobase.model;

import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.persistence.AnnotationDescriptor;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class SaveContext<T extends Base> {
  private T model;
  private T original;
  private Repository<T> repository;
  private PropertyContainer node;
  private List<FieldDescriptor> changedFields;
  private List<FieldDescriptor> fieldMap;

  public SaveContext(Repository<T> repository, T model) {
    this.repository = repository;
    this.model = model;
    if(model.getId() > 0) {
      this.original = repository.createModel(node(), FieldList.parseFields(Base.AllFields));
    }
    else if(model.getUuid() != null) {
      this.original = repository.findByUuid(model.getUuid());
      this.original = repository.fetch(original, Base.AllFields);
      this.model.setId(this.original.getId());
    }
    fieldMap = Persistence.cache().fieldMap(model.getClass());
  }

  public List<FieldDescriptor> changedFields() {
    if(changedFields == null) {
      changedFields = fieldMap.stream()
        .filter(f-> fieldChanged(f.field()))
        .collect(Collectors.toList());
    }
    return changedFields;
  }
  
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
        if(original == null) {
          return true;
        }
        else {
          if(annotation.relatedTo != null && annotation.lazy) return true;
          if(annotation.relatedTo != null) repository().fetch(original, FieldList.parseFields(field.getName()+"(max)"));
          Object originalValue = field.get(original);
          return !value.equals(originalValue);
        }
      }
    } catch(IllegalAccessException e) {
      throw new NeobaseRuntimeException(e.getMessage(), e);
    }
    
    return false;
  }
  
  public List<FieldDescriptor> fieldMap() {
    return fieldMap;
  }

  public T model() {
    return model;
  }

  public Repository<T> repository() {
    return repository;
  }

  public PropertyContainer node() {
    if(node == null && original() != null) {
      if(model instanceof Model) node = repository.graph().getNodeById(original().getId());
      else node = repository.graph().getRelationshipById(original().getId());
    } else if(node == null && model().getId() > 0) {
      if(model instanceof Model) node = repository.graph().getNodeById(model().getId());
      else node = repository.graph().getRelationshipById(model().getId());
    }
    return node;
  }

  public T original() {
    return original;
  }

  public void setNode(Node node) {
    this.node = node;
  }
}
