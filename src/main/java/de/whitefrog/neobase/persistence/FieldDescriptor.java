package de.whitefrog.neobase.persistence;

import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.annotation.*;
import de.whitefrog.neobase.model.relationship.Relationship;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldDescriptor {
  private AnnotationDescriptor annotations;
  private Field field;
  private boolean collection;
  private boolean model;
  private boolean relationship;
  private Class<? extends Base> baseClass;

  public FieldDescriptor(Field field) {
    field.setAccessible(true);
    AnnotationDescriptor descriptor = new AnnotationDescriptor();
    descriptor.indexed = field.getAnnotation(Indexed.class);
    descriptor.notPersistant = field.isAnnotationPresent(NotPersistant.class);
    descriptor.relatedTo = field.getAnnotation(RelatedTo.class);
    descriptor.unique = field.isAnnotationPresent(Unique.class);
    descriptor.fetch = field.isAnnotationPresent(Fetch.class);
    descriptor.required = field.isAnnotationPresent(Required.class);
    descriptor.nullRemove = field.isAnnotationPresent(NullRemove.class);
    descriptor.blob = field.isAnnotationPresent(Blob.class);
    descriptor.uuid = field.isAnnotationPresent(Uuid.class);
    descriptor.lazy = field.isAnnotationPresent(Lazy.class);
    descriptor.relationshipCount = field.getAnnotation(RelationshipCount.class);
    
    this.field = field;
    this.annotations = descriptor;
    this.collection = Collection.class.isAssignableFrom(field.getType());
    if(this.collection) {
      this.baseClass = (Class<? extends Base>) ReflectionUtil.getGenericClass(field);
    } else {
      this.baseClass = (Class<? extends Base>) field.getType();
    }
    model = Model.class.isAssignableFrom(baseClass);
    relationship = Relationship.class.isAssignableFrom(baseClass);
  }
  
  public AnnotationDescriptor annotations() {
    return annotations;
  }
  public boolean isCollection() {
    return collection;
  }
  public Class<? extends Base> baseClass() {
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
}
