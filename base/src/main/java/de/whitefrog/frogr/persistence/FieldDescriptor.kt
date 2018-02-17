package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.helper.ReflectionUtil
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.model.annotation.*
import de.whitefrog.frogr.model.relationship.Relationship

import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
class FieldDescriptor<T : Base> internal constructor(private val field: Field) {
  private val annotations: AnnotationDescriptor
  val isCollection: Boolean
  val isRelationship: Boolean
  private var baseClass: Class<T>
  private var baseClassName: String
  private val f = field
  val name: String
    get() { return f.name }

  val isModel: Boolean
    get() = Model::class.java.isAssignableFrom(baseClass)

  init {
    field.isAccessible = true
    this.isCollection = Collection::class.java.isAssignableFrom(field.type)

    val descriptor = AnnotationDescriptor()
    descriptor.indexed = field.getAnnotation(Indexed::class.java)
    descriptor.notPersistent = field.isAnnotationPresent(NotPersistent::class.java)
    descriptor.relatedTo = field.getAnnotation(RelatedTo::class.java)
    descriptor.unique = field.isAnnotationPresent(Unique::class.java)
    descriptor.fetch = field.isAnnotationPresent(Fetch::class.java)
    descriptor.required = field.isAnnotationPresent(Required::class.java)
    descriptor.nullRemove = field.isAnnotationPresent(NullRemove::class.java)
    descriptor.blob = field.isAnnotationPresent(Blob::class.java)
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
  }

  fun annotations(): AnnotationDescriptor {
    return annotations
  }

  fun baseClass(): Class<T> {
    return baseClass
  }

  fun baseClassName(): String {
    return baseClassName
  }

  fun field(): Field {
    return field
  }

  override fun toString(): String {
    return "Field: \"" + name + "\""
  }
}
