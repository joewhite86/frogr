package de.whitefrog.frogr.persistence;

import de.whitefrog.frogr.model.Base;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Model cache to reduce reflection usage for models to a minimum.
 */
public class ModelCache {
  private Map<Class, List<FieldDescriptor>> cache = new HashMap<>();
  private Map<String, Class> modelCache = new HashMap<>();
  private List<String> ignoreFields = Arrays.asList(
    "id", "initialId", "checkedFields", "fetchedFields");
  private Reflections reflections;

  public ModelCache(Collection<String> packages) {
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
      .setScanners(new SubTypesScanner());
    packages
      .forEach(pkg -> configurationBuilder.addUrls(ClasspathHelper.forPackage(pkg)));
    reflections = new Reflections(configurationBuilder);
    for(Class clazz : reflections.getSubTypesOf(Base.class)) {
      modelCache.put(clazz.getSimpleName(), clazz);
    }
  }
  
  public List<Class> subTypesOf(Class<?> baseClass) {
    return new ArrayList<>(reflections.getSubTypesOf(baseClass));
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
            !Modifier.isStatic(field.getModifiers()) &&
            !containsField(descriptors, field.getName())) {
          descriptors.add(new FieldDescriptor<>(field));
        }
      }
      traverse = traverse.getSuperclass();
    }

    cache.put(clazz, descriptors);

    return descriptors;
  }
  
  private <M extends Base> boolean containsField(List<FieldDescriptor> descriptors, String fieldName) {
    for(FieldDescriptor<M> descriptor : descriptors) {
      if(descriptor.field().getName().equals(fieldName)) return true;
    }
    return false;
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
