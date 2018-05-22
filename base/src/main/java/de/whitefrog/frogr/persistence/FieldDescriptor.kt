package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.helper.ReflectionUtil
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.model.annotation.*
import de.whitefrog.frogr.model.relationship.Relationship

import java.lang.reflect.Field

/**
 * Describes a field on any model and provides easy access to its annotations.
 * Provides methods to check if a field is a collection or a single model.
 */
@Suppress("UNCHECKED_CAST")
class FieldDescriptor<T : Base> internal constructor(private val field: Field) {
  private val annotations: AnnotationDescriptor
  private var baseClass: Class<T>
  private var baseClassName: String
  private val f = field

  /**
   * Indicates that the field is a model type or a collection of models.
   */
  val isModel: Boolean
  /**
   * Indicates that the field is a collection.
   */
  val isCollection: Boolean
  /**
   * Indicates that the field is a relationship type or a collection of relationships.
   */
  val isRelationship: Boolean

  /**
   * Get the fields name.
   */
  val name: String
    get() { return f.name }

  init {
    field.isAccessible = true
    this.isCollection = Collection::class.java.isAssignableFrom(field.type)

    val descriptor = AnnotationDescriptor()
    descriptor.indexed = field.getAnnotation(Indexed::class.java)
    descriptor.notPersistent = field.isAnnotationPresent(NotPersistent::class.java)
    descriptor.relatedTo = field.getAnnotation(RelatedTo::class.java)
    descriptor.unique = field.isAnnotationPresent(Unique::class.java)
    descriptor.fetch = field.getAnnotation(Fetch::class.java)
    descriptor.required = field.isAnnotationPresent(Required::class.java)
    descriptor.nullRemove = field.isAnnotationPresent(NullRemove::class.java)
    descriptor.uuid = field.isAnnotationPresent(Uuid::class.java)
    descriptor.lazy = field.isAnnotationPresent(Lazy::class.java)
    descriptor.relationshipCount = field.getAnnotation(RelationshipCount::class.java)

    this.annotations = descriptor

    if (this.isCollection) {
      this.baseClass = ReflectionUtil.getGenericClass(field) as Class<T>
    } else {
      this.baseClass = field.type as Class<T>
    }
    this.baseClassName = this.baseClass.simpleName

    this.isRelationship = Relationship::class.java.isAssignableFrom(baseClass)
    this.isModel = Model::class.java.isAssignableFrom(baseClass)
  }

  /**
   * Get the annotation descriptor for the described field.
   * @return the annotation descriptor for the described field
   */
  fun annotations(): AnnotationDescriptor {
    return annotations
  }

  /**
   * Get the fields class, or if it's a collection, the generic type.
   * @return the fields class, or if it's a collection, the generic type
   */
  fun baseClass(): Class<T> {
    return baseClass
  }

  /**
   * Get the base class's name.
   * @return the name of the base class
   */
  fun baseClassName(): String {
    return baseClassName
  }

  /**
   * Get the reflected field.
   * @return the reflected field
   */
  fun field(): Field {
    return field
  }

  override fun toString(): String {
    return "Field: \"" + name + "\""
  }
}
