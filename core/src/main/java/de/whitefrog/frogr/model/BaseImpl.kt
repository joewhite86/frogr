package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnore
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
  override var id = random.nextLong()
    get() {
      return if (initialId) -1 else field
    }
    set(value) {
      field = value
      initialId = false
    }
  @NotPersistent
  @JsonIgnore
  override var checkedFields: HashSet<String> = HashSet()
  @NotPersistent
  @JsonIgnore
  override var fetchedFields: HashSet<String> = HashSet()
  @NotPersistent
  @JsonIgnore
  override var removeProperties: HashSet<String> = HashSet()

  /**
   * Reset the id to a random one.
   * Sets initialId to true too.
   */
  override fun resetId() {
    id = random.nextLong()
    initialId = true
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

  override fun toString(): String {
    val typeName = javaClass.simpleName
    return "$typeName ($id)"
  }
}