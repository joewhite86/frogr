package de.whitefrog.neobase.persistence;

import de.whitefrog.neobase.model.Base;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ModelCache {
  private Map<Class, List<FieldDescriptor>> cache = new HashMap<>();
  private Map<String, Class> modelCache = new HashMap<>();
  private List<String> ignoreFields = Arrays.asList(
    "id", "initialId", "checkedFields", "fetchedFields");
  private List<Reflections> reflections = new ArrayList<>();

  public ModelCache(Collection<String> packages) {
    for(String pkg: packages) {
      Reflections reflections = new Reflections(pkg);
      this.reflections.add(reflections);
      for(Class clazz: reflections.getSubTypesOf(Base.class)) {
        modelCache.put(clazz.getSimpleName(), clazz);
      }
    }
  }
  
  public List<Class> subTypesOf(Class<?> baseClass) {
    List<Class> subTypes = new ArrayList<>();
    for(Reflections reflection: reflections) {
      subTypes.addAll(reflection.getSubTypesOf(baseClass));
    }
    return subTypes;
  }

  public AnnotationDescriptor fieldAnnotations(Class clazz, String fieldName) {
    FieldDescriptor descriptor = fieldDescriptor(clazz, fieldName);
    return descriptor != null? descriptor.annotations(): null;
  }
  
  public FieldDescriptor fieldDescriptor(Class clazz, String fieldName) {
    String firstField = fieldName.contains(".")? fieldName.substring(0, fieldName.indexOf(".")): fieldName;
    FieldDescriptor descriptor = null;
    for(FieldDescriptor subDescriptor: fieldMap(clazz)) {
      if(firstField.equals(subDescriptor.field().getName())) {
        descriptor = subDescriptor;
        break;
      } 
    }
    if(descriptor != null && firstField.length() < fieldName.length()) {
      return fieldDescriptor(descriptor.baseClass(), 
        fieldName.substring(fieldName.indexOf(".") + 1, fieldName.length()));
    }
    return descriptor;
  }
  
  public FieldDescriptor fieldDescriptor(Field field) {
    return fieldDescriptor(field.getDeclaringClass(), field.getName());
  }

  public List<FieldDescriptor> fieldMap(Class clazz) {
    if(cache.containsKey(clazz)) return cache.get(clazz);

    List<FieldDescriptor> descriptors = new ArrayList<>();
    Class traverse = clazz;

    while(traverse != null && Base.class.isAssignableFrom(traverse)) {
      for(Field field : traverse.getDeclaredFields()) {
        if(!ignoreFields.contains(field.getName()) &&
           !Modifier.isStatic(field.getModifiers())) {
          descriptors.add(new FieldDescriptor<>(clazz, field));
        }
      }
      traverse = traverse.getSuperclass();
    }

    cache.put(clazz, descriptors);

    return descriptors;
  }

  public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> tmpClass = clazz;
    do {
      try {
        return tmpClass.getDeclaredField(fieldName);
      } catch(NoSuchFieldException e) {
        tmpClass = tmpClass.getSuperclass();
      }
    } while(tmpClass != null);

    throw new NoSuchFieldException("Field '" + fieldName + "' not found on class " + clazz);
  }

  public boolean containsModel(String name) {
    return modelCache.containsKey(name);
  }

  public Collection<Class> getAllModels() {
    return modelCache.values();
  }
  
  public Class getModel(String name) {
    return modelCache.get(name);
  }

}
