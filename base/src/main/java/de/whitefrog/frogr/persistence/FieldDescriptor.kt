package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.helper.ReflectionUtil
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.model.annotation.*
import de.whitefrog.frogr.model.relationship.Relationship

import java.lang.reflect.Field

class FieldDescriptor<T : Base> internal constructor(private val f: Field) {
  private val annotations: AnnotationDescriptor
  val isCollection: Boolean
  val isRelationship: Boolean
  private var baseClass: Class<T>
  private var baseClassName: String
  val name: String
    get() { return f.name }

  val isModel: Boolean
    get() = Model::class.java.isAssignableFrom(baseClass)

  init {
    f.isAccessible = true
    this.isCollection = Collection::class.java.isAssignableFrom(f.type)

    val descriptor = AnnotationDescriptor()
    descriptor.indexed = f.getAnnotation(Indexed::class.java)
    descriptor.notPersistent = f.isAnnotationPresent(NotPersistent::class.java)
    descriptor.relatedTo = f.getAnnotation(RelatedTo::class.java)
    descriptor.unique = f.isAnnotationPresent(Unique::class.java)
    descriptor.fetch = f.isAnnotationPresent(Fetch::class.java)
    descriptor.required = f.isAnnotationPresent(Required::class.java)
    descriptor.nullRemove = f.isAnnotationPresent(NullRemove::class.java)
    descriptor.blob = f.isAnnotationPresent(Blob::class.java)
    descriptor.uuid = f.isAnnotationPresent(Uuid::class.java)
    descriptor.lazy = f.isAnnotationPresent(Lazy::class.java)
    descriptor.relationshipCount = f.getAnnotation(RelationshipCount::class.java)

    this.annotations = descriptor

    if (this.isCollection) {
      this.baseClass = ReflectionUtil.getGenericClass(f) as Class<T>
    } else {
      this.baseClass = f.type as Class<T>
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
    return f
  }

  override fun toString(): String {
    return "Field: \"" + name + "\""
  }
}
