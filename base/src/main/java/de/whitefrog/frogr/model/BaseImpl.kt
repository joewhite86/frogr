package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.annotation.Fetch
import de.whitefrog.frogr.model.annotation.NotPersistent
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

abstract class BaseImpl: Base, Comparable<Base> {
  companion object {
    internal val random = Random()
  }
  /**
   * True, if the id was set automatically. False when set by a database.
   */
  @NotPersistent
  protected var initialId = true
  /**
   * Type identifier. Used to determine the model class to use for this entity.
   */
  @Fetch
  override var type: String? = null
  /**
   * Unique identifier. Not guaranteed unique.
   * Will be -1 when no id is set yet.
   */
  override var id = random.nextLong()
    get() {
      return if (initialId) -1 else field
    }
    set(value) {
      field = value
      initialId = false
    }

  /**
   * Add a field that already was checked.
   * @param field Field name
   */
  override fun addCheckedField(field: String) {
    checkedFields.add(field)
  }
  /**
   * Already checked fields during a save process. Used to prevent
   * double checking.
   */
  @NotPersistent
  @JsonIgnore
  override var checkedFields: HashSet<String> = HashSet()
  /**
   * Fields already fetched from database. Used to prevent unneccecary
   * read actions.
   */
  @NotPersistent
  @JsonIgnore
  override var fetchedFields: HashSet<String> = HashSet()

  @NotPersistent
  @JsonIgnore
  override var removeProperties: HashSet<String> = HashSet()

  override fun removeProperty(property: String) {
    removeProperties.add(property)
  }

  /**
   * True, if the entity was already persisted.
   */
  override val isPersisted: Boolean
    @JsonIgnore
    get() = id > -1
  
  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  override fun <T:Base> clone(vararg fields: String): T {
    return clone(Arrays.asList(*fields))
  }

  
  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T:Base> clone(fields: List<String>): T {
    val base: T
    try {
      base = javaClass.newInstance() as T
      base.type = type()
      if(!initialId) base.id = id
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
   * Hash code method. Returns a hash built from important entity properties.
   */
  override fun hashCode(): Int {
    return HashCodeBuilder(17, 23)
      .append(id)
      .toHashCode()
  }

  /**
   * Tests if two entities are equal.
   * This is used in Lists etc. to ensure only single entities.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BaseImpl) return false

    if (initialId) return false
    if (id != other.id) return false

    if (javaClass != other.javaClass) return false

    return true
  }

  /**
   * Compare two entities. Primarily compares the ids.
   */
  override fun compareTo(other: Base): Int {
    return java.lang.Long.compare(id, other.id)
  }

  /**
   * Reset the id to a random one.
   * Sets initialId to true too.
   */
  override fun resetId() {
    id = random.nextLong()
    initialId = true
  }

  override fun toString(): String {
    val typeName = if (type() != null) type() else javaClass.simpleName
    return "$typeName ($id)"
  }
}