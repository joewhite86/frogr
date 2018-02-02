package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.annotation.Fetch
import de.whitefrog.frogr.model.annotation.NotPersistant
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.model.annotation.Uuid
import de.whitefrog.frogr.persistence.Persistence
import de.whitefrog.frogr.rest.Views
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

/**
 * Abstract base class for all used entities.
 * Provides convinient basic properties and methods common to all entities.
 */
abstract class Entity : Model, Comparable<Base> {
  /**
   * Unique identifier. Not guaranteed unique. 
   * Will be -1 when no id is set yet.
   */
  @JsonView(Views.Hidden::class)
  override var id = random.nextLong()
    get() {
      return if (initialId) -1 else field
    }
    set(value) {
      field = value
      initialId = false
    }
  /**
   * Unique identifier. String based, guaranteed unique identifier.
   */
  @Uuid @Fetch @Unique 
  override var uuid: String? = null
  /**
   * Type identifier. Used to determine the model class to use for this entity.
   */
  @Fetch 
  override var type: String? = null
  /**
   * Model to use. Can be used for abstract parent classes to further determine 
   * the correct type.
   */
  @JsonView(Views.Hidden::class)
  override var model: String? = null
  /**
   * Timestamp, automatically set on first persist.
   */
  @JsonView(Views.Secure::class)
  override var created: Long? = null
  /**
   * Timestamp, updated each time the entity is perstisted.
   */
  @JsonView(Views.Secure::class)
  private var lastModified: Long? = null
    private set(value) {
      field = value
    }
  /**
   * String to display who updated the entity.
   */
  @JsonView(Views.Hidden::class)
  override var modifiedBy: String? = null

  /**
   * True, if the id was set automatically. False when set by a database.
   */
  @NotPersistant
  private var initialId = true
  /**
   * Already checked fields during a save process. Used to prevent
   * double checking.
   */
  @NotPersistant
  @JsonIgnore
  override var checkedFields: HashSet<String> = HashSet()
  /**
   * Fields already fetched from database. Used to prevent unneccecary
   * read actions.
   */
  @NotPersistant
  @JsonIgnore
  override var fetchedFields: HashSet<String> = HashSet()

  @NotPersistant
  @JsonIgnore
  override var removeProperties: HashSet<String> = HashSet()

  override fun removeProperty(property: String) {
    removeProperties.add(property)
  }
  
  /**
   * True, if the entity was already persisted.
   */
  override val persisted: Boolean
    @JsonIgnore
    get() = id > -1 || uuid != null

  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  override fun <T : Base> clone(vararg fields: String): T {
    return clone(Arrays.asList(*fields))
  }

  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  override fun <T : Base> clone(fields: List<String>): T {
    val base: T
    try {
      base = javaClass.newInstance() as T
      base.type = type()
      base.id = id
      base.uuid = uuid
      for (descriptor in Persistence.cache().fieldMap(javaClass)) {
        if (fields.contains(descriptor.name) || descriptor.annotations().relatedTo == null && fields.contains(Entity.AllFields)) {
          descriptor.field().isAccessible = true
          descriptor.field().set(base, descriptor.field().get(this))
        }
      }
    } catch (e: ReflectiveOperationException) {
      throw FrogrException(e.message, e)
    }

    return base
  }

  /**
   * Returns the entity type.
   */
  override fun type(): String? {
    return type
  }

  /**
   * Add a field that already was checked.
   * @param field Field name
   */
  override fun addCheckedField(field: String) {
    checkedFields.add(field)
  }

  /**
   * Get the last modified timestamp.
   * @return Last modified timestamp
   */
  override fun getLastModified(): Long? {
    return lastModified
  }

  /**
   * Set the last modified timestamp.
   * @param lastModified Timestamp
   */
  fun setLastModified(lastModified: Long) {
    this.lastModified = lastModified
  }

  /**
   * Update the last modified timestamp to current.
   */
  override fun updateLastModified() {
    this.lastModified = System.currentTimeMillis()
  }

  /**
   * Reset the id to a random one.
   * Sets initialId to true too.
   */
  override fun resetId() {
    id = random.nextLong()
    initialId = true
  }

  /**
   * Compare two entities. Primarily compares the ids.
   */
  override fun compareTo(other: Base): Int {
    return java.lang.Long.compare(id, other.id)
  }

  /**
   * Tests if two entities are equal. 
   * This is used in Lists etc. to ensure only single entities.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Entity) return false

    val base = other

    if (uuid != null && base.uuid != null) {
      if (uuid != base.uuid) return false
    }
    if (initialId) return false
    if (id != base.id) return false

    if (javaClass != base.javaClass) return false

    return true
  }

  /**
   * Hash code method. Returns a hash built from important entity properties.
   */
  override fun hashCode(): Int {
    return HashCodeBuilder(17, 37)
      .append(id)
      .toHashCode()
  }

  override fun toString(): String {
    val typeName = if (type() != null) type() else javaClass.simpleName
    val id = if (id == -1L) uuid else id.toString()
    return "$typeName ($id)"
  }

  companion object {
    @JvmField val AllFields = "all"
    @JvmField val GravatarUrl = "gravatarUrl"
    @JvmField val IdProperty = "id"
    @JvmField val ModifiedBy = "modifiedBy"
    @JvmField val LastModified = "lastModified"
    @JvmField val Created = "created"
    @JvmField val Type = "type"
    @JvmField val Model = "model"
    @JvmField val Uuid = "uuid"
    private val random = Random()
  }
}
