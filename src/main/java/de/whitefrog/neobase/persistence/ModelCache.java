package de.whitefrog.neobase.persistence;

import de.whitefrog.neobase.model.Base;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ModelCache {
  private Map<Class, List<FieldDescriptor>> cache = new HashMap<>();
  private Map<String, Class> modelCache = new HashMap<>();

  public ModelCache(Collection<String> packages) {
    for(String pkg: packages) {
      Reflections reflections = new Reflections(pkg);
      for(Class clazz: reflections.getSubTypesOf(Base.class)) {
        modelCache.put(clazz.getSimpleName(), clazz);
      }
    }
  }

  public AnnotationDescriptor fieldAnnotations(Class clazz, String fieldName) {
    return fieldDescriptor(clazz, fieldName).annotations();
  }
  
  public FieldDescriptor fieldDescriptor(Class clazz, String fieldName) {
    for(FieldDescriptor descriptor: fieldMap(clazz)) {
      if(fieldName.equals(descriptor.field().getName())) return descriptor; 
    }
    return null;
  }

  public List<FieldDescriptor> fieldMap(Class clazz) {
    if(cache.containsKey(clazz)) return cache.get(clazz);

    List<FieldDescriptor> descriptors = new ArrayList<>();
    Class traverse = clazz;

    while(traverse != null && Base.class.isAssignableFrom(traverse)) {
      for(Field field : traverse.getDeclaredFields()) {
        if(!field.getName().equals("id") &&
           !field.getName().equals("fetchedFields") &&
           !Modifier.isStatic(field.getModifiers())) {
          descriptors.add(new FieldDescriptor(field));
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

  public Class getModel(String name) {
    return modelCache.get(name);
  }

}
