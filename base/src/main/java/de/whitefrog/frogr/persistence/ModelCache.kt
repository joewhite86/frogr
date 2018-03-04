package de.whitefrog.frogr.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.mrbean.MrBeanModule
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.relationship.Relationship
import org.apache.commons.lang3.reflect.FieldUtils
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * Model cache to reduce reflection usage for models to a minimum.
 * Scans all registered packages for classes implementing the {@link Base}
 * interface, reads all fields and creates a descriptor (see {@link FieldDescriptor}) map.
 * 
 * <p>Provides methods for easy access to models and their descriptors.</p>
 */
class ModelCache {
  companion object {
    private val logger = LoggerFactory.getLogger(ModelCache::class.java)
  }
  private val cache = HashMap<Class<*>, List<FieldDescriptor<*>>>()
  private val modelCache = HashMap<String, Class<*>>()
  private val ignoreFields = Arrays.asList(
    "id", "initialId", "checkedFields", "fetchedFields")
  private lateinit var reflections: Reflections

  /**
   * Get all registered models as {@link Collection}.
   * @return all registered models as collection.
   */
  val allModels: Collection<Class<*>>
    get() = modelCache.values

  private fun containsField(descriptors: List<FieldDescriptor<*>>, fieldName: String): Boolean {
    return descriptors.stream().anyMatch { descriptor -> descriptor.field().name == fieldName }
  }

  /**
   * Tests if a model with a certain name is registered.
   */
  fun containsModel(name: String): Boolean {
    return modelCache.containsKey(name)
  }

  /**
   * Get the reflected field of a given class, checks superclasses too.
   * @param clazz the model class
   * @param fieldName the fields name
   * @return the reflected field, when found
   * @throws NoSuchFieldException when the field cannot be found
   */
  @Throws(NoSuchFieldException::class)
  fun getField(clazz: Class<*>, fieldName: String): Field {
    var tmpClass: Class<*>? = clazz
    do {
      try {
        return tmpClass!!.getDeclaredField(fieldName)
      } catch (e: NoSuchFieldException) {
        tmpClass = tmpClass!!.superclass
      }

    } while (tmpClass != null)

    throw NoSuchFieldException("Field '$fieldName' not found on class $clazz")
  }

  /**
   * Scan for models and parse them.
   */
  fun scan(packages: Collection<String>) {
    modelCache.clear()
    val configurationBuilder = ConfigurationBuilder()
      .setScanners(SubTypesScanner())
    packages
      .forEach { pkg -> configurationBuilder.addUrls(ClasspathHelper.forPackage(pkg)) }
    reflections = Reflections(configurationBuilder)
    val mapper = ObjectMapper()
    // com.fasterxml.jackson.module.mrbean.MrBeanModule:
    mapper.registerModule(MrBeanModule())

    for (clazz in reflections.getSubTypesOf(Base::class.java)) {
      //      if(clazz.isInterface()) continue;
      modelCache[clazz.simpleName] = clazz

      val fields: List<Field>
      if (clazz.isInterface) {
        try {
          val instance = mapper.readValue("{}", clazz)
          fields = FieldUtils.getAllFieldsList(instance.javaClass)
        } catch (e: IOException) {
          throw FrogrException("could not parse interface " + clazz.simpleName)
        }
      } else {
        fields = FieldUtils.getAllFieldsList(clazz)
      }
      val descriptors = ArrayList<FieldDescriptor<*>>()
      for(field in fields) {
        if(ignoreFields.contains(field.name)) continue
        if(Modifier.isStatic(field.modifiers)) continue
        if(containsField(descriptors, field.name)) continue
        descriptors.add(FieldDescriptor<Base>(field))
      }

      cache[clazz] = descriptors
    }

    validateAnnotations()
  }

  private fun validateAnnotations() {
    for (modelClass in cache.keys) {
      for (descriptor in cache[modelClass]!!) {
        val annotations = descriptor.annotations()
        
        // check annotations for validity
        if (annotations.indexed != null && annotations.relatedTo != null) {
          logger.error("annotations @Indexed and @RelatedTo should not be used together ({}->{})",
            modelClass.simpleName, descriptor.name)
        }
        if (annotations.nullRemove && annotations.required) {
          logger.error("annotations @NullRemove and @Required should not be used together ({}->{})",
            modelClass.simpleName, descriptor.name)
        }
        
        // validate relationship models
        if (modelClass is Relationship<*,*>) {
          if (annotations.relatedTo != null) {
            logger.error("annotation @RelatedTo is not allowed in relationship models ({}->{})",
              modelClass.simpleName, descriptor.name)
          }
          if (annotations.relationshipCount != null) {
            logger.error("annotation @RelationshipCount is not allowed in relationship models ({}->{})",
              modelClass.simpleName, descriptor.name)
          }
          if (annotations.indexed != null) {
            logger.error("annotation @Indexed is not allowed in relationship models ({}->{})",
              modelClass.simpleName, descriptor.name)
          }
          if (annotations.unique) {
            logger.error("annotation @Unique is not allowed in relationship models ({}->{})",
              modelClass.simpleName, descriptor.name)
          }
        }
      }
    }
  }

  fun subTypesOf(baseClass: Class<*>): List<Class<*>> {
    return ArrayList(reflections.getSubTypesOf(baseClass))
  }

  fun fieldAnnotations(clazz: Class<*>, fieldName: String): AnnotationDescriptor? {
    val descriptor = fieldDescriptor(clazz, fieldName)
    return descriptor?.annotations()
  }

  fun fieldDescriptor(field: Field): FieldDescriptor<*>? {
    return fieldDescriptor(field.declaringClass, field.name)
  }

  fun fieldDescriptor(clazz: Class<*>, fieldName: String): FieldDescriptor<*>? {
    val firstField = if (fieldName.contains(".")) fieldName.substring(0, fieldName.indexOf(".")) else fieldName
    val descriptor: FieldDescriptor<*>? = fieldMap(clazz).firstOrNull { firstField == it.field().name }
    return if (descriptor != null && firstField.length < fieldName.length) {
      fieldDescriptor(descriptor.baseClass(),
        fieldName.substring(fieldName.indexOf(".") + 1, fieldName.length))
    } else descriptor
  }

  fun fieldMap(clazz: Class<*>): List<FieldDescriptor<*>> {
    return cache[clazz]!!
  }

  fun getModel(name: String): Class<*>? {
    return modelCache[name]
  }

  fun getModelName(modelClass: Class<*>): String? {
    return modelCache.keys.firstOrNull { modelClass == modelCache[it] }
  }
}
