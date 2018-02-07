package de.whitefrog.frogr.persistence;

import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.Base;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.annotation.*;
import de.whitefrog.frogr.model.relationship.Relationship;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldDescriptor<T extends Base> {
  private AnnotationDescriptor annotations;
  private Field field;
  private boolean collection;
  private boolean model;
  private boolean relationship;
  private Class<T> baseClass;

  public FieldDescriptor(Field field) {
    field.setAccessible(true);
    this.field = field;
    this.collection = Collection.class.isAssignableFrom(field.getType());
    
    AnnotationDescriptor descriptor = new AnnotationDescriptor();
    descriptor.indexed = field.getAnnotation(Indexed.class);
    descriptor.notPersistent = field.isAnnotationPresent(NotPersistent.class);
    descriptor.relatedTo = field.getAnnotation(RelatedTo.class);
    descriptor.unique = field.isAnnotationPresent(Unique.class);
    descriptor.fetch = field.isAnnotationPresent(Fetch.class);
    descriptor.required = field.isAnnotationPresent(Required.class);
    descriptor.nullRemove = field.isAnnotationPresent(NullRemove.class);
    descriptor.blob = field.isAnnotationPresent(Blob.class);
    descriptor.uuid = field.isAnnotationPresent(Uuid.class);
    descriptor.lazy = field.isAnnotationPresent(Lazy.class);
    descriptor.relationshipCount = field.getAnnotation(RelationshipCount.class);

    this.annotations = descriptor;
    
    if(this.collection) {
      this.baseClass = (Class<T>) ReflectionUtil.getGenericClass(field);
    } else {
      this.baseClass = (Class<T>) field.getType();
    }
    
    this.model = Model.class.isAssignableFrom(baseClass);
    this.relationship = Relationship.class.isAssignableFrom(baseClass);
  }
  
  public AnnotationDescriptor annotations() {
    return annotations;
  }
  public boolean isCollection() {
    return collection;
  }
  public Class<T> baseClass() {
    return baseClass;
  }
  public Field field() {
    return field;
  }
  public String getName() {
    return field.getName();
  }

  public boolean isModel() {
    return Model.class.isAssignableFrom(baseClass);
  }

  public boolean isRelationship() {
    return relationship;
  }

  @Override
  public String toString() {
    return "Field: \"" + getName() + "\"";
  }
}
