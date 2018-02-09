package de.whitefrog.frogr.persistence;

import de.whitefrog.frogr.model.Base;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Model cache to reduce reflection usage for models to a minimum.
 */
public class ModelCache {
  private static final Logger logger = LoggerFactory.getLogger(ModelCache.class);
  private Map<Class, List<FieldDescriptor>> cache = new HashMap<>();
  private Map<String, Class> modelCache = new HashMap<>();
  private List<String> ignoreFields = Arrays.asList(
    "id", "initialId", "checkedFields", "fetchedFields");
  private Reflections reflections;
  
  public void scan(Collection<String> packages) {
    modelCache.clear();
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
      .setScanners(new SubTypesScanner());
    packages
      .forEach(pkg -> configurationBuilder.addUrls(ClasspathHelper.forPackage(pkg)));
    reflections = new Reflections(configurationBuilder);
    for(Class clazz : reflections.getSubTypesOf(Base.class)) {
      modelCache.put(clazz.getSimpleName(), clazz);

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
    }
    
    validateAnnotations();
  }
  
  private void validateAnnotations() {
    for(Class modelClass: cache.keySet()) {
      for(FieldDescriptor descriptor: cache.get(modelClass)) {
        AnnotationDescriptor annotations = descriptor.annotations();
        // check annotations for validity
        if(annotations.indexed != null && annotations.relatedTo != null) {
          logger.warn("annotations @Indexed and @RelatedTo should not be used together ({}->{})",
            modelClass.getSimpleName(), descriptor.getName());
        }
        if(annotations.nullRemove && annotations.required) {
          logger.warn("annotations @NullRemove and @Required should not be used together ({}->{})",
            modelClass.getSimpleName(), descriptor.getName());
        }
      }
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
    return cache.get(clazz);
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
