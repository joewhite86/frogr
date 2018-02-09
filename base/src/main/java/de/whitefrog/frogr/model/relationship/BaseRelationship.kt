package de.whitefrog.frogr.model.relationship

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.model.annotation.Fetch
import de.whitefrog.frogr.model.annotation.NotPersistent
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.model.annotation.Uuid
import de.whitefrog.frogr.rest.Views
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

/**
 * Base class for all relationships between entities.
 * Cannot be abstract, because it is used in DefaultRelationshipRepository as default
 */
open class BaseRelationship<From : Model, To : Model>() : Relationship<From, To> {
  final override lateinit var from: From
  final override lateinit var to: To
  
  constructor(from: From, to: To): this() {
    this.from = from
    this.to = to
  }
  
  @JsonView(Views.Hidden::class)
  override var id = random.nextLong()
    get() {
      return if (initialId) -1 else field
    }
    set(value) {
      field = value
      initialId = false
    }
  @Uuid @Unique @Fetch
  override var uuid: String? = null
  @Fetch
  override var type: String? = null
  override var created: Long? = null
  @JsonView(Views.Secure::class)
  private var lastModified: Long? = null

  @NotPersistent
  private var initialId = true
  @NotPersistent
  @JsonIgnore
  override var checkedFields: HashSet<String> = HashSet()
  @NotPersistent
  @JsonIgnore
  override var fetchedFields: HashSet<String> = HashSet()
  @NotPersistent
  @JsonIgnore
  override var removeProperties: HashSet<String> = HashSet()

  override fun resetId() {
    id = random.nextLong()
    initialId = true
  }

  override fun type(): String? {
    return type
  }

  override fun addCheckedField(field: String) {
    checkedFields.add(field)
  }
  
  override fun removeProperty(property: String) {
    removeProperties.add(property)
  }

  override fun getLastModified(): Long? {
    return lastModified
  }

  override fun updateLastModified() {
    this.lastModified = System.currentTimeMillis()
  }

  override val persisted: Boolean
    get() = id > -1 || uuid != null

  override fun <T : Base> clone(vararg fields: String): T {
    return clone(Arrays.asList(*fields))
  }

  override fun <T : Base> clone(fields: List<String>): T {
    val base: T
    try {
      base = javaClass.newInstance() as T
    } catch (e: ReflectiveOperationException) {
      throw FrogrException(e.message, e)
    }

    base.type = type()
    if (fields.isEmpty() || fields.contains(IdProperty) && id > -1) base.id = id
    return base
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Relationship<*, *>) return false

    val base = other as BaseRelationship<*, *>? ?: return false

    if (uuid != null && base.uuid != null) {
      if (uuid != base.uuid) return false
    }
    if (initialId) return false
    if (id != base.id) return false

    if (javaClass != base.javaClass) return false

    return true
  }

  override fun hashCode(): Int {
    return HashCodeBuilder(21, 41)
      .append(id)
      .toHashCode()
  }

  override fun toString(): String {
    val typeName = if (type() != null) type() else javaClass.simpleName
    return "$typeName ($id)"
  }

  companion object {
    @JvmField val AllFields = "all"
    @JvmField val IdProperty = "id"
    @JvmField val LastModified = "lastModified"
    @JvmField val Created = "created"
    @JvmField val Type = "type"
    @JvmField val Uuid = "uuid"
    private val random = Random()
  }
}
