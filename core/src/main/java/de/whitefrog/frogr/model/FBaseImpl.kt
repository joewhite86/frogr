package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.model.annotation.Fetch
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.model.annotation.Uuid
import de.whitefrog.frogr.rest.Views
import org.apache.commons.lang3.builder.HashCodeBuilder

abstract class FBaseImpl : BaseImpl(), FBase {
  @JsonView(Views.Hidden::class)
  override var id = random.nextLong()
    get() {
      return if (initialId) -1 else field
    }
    set(value) {
      field = value
      initialId = false
    }
  override val isPersisted: Boolean
    @JsonIgnore
    get() = id > -1 || uuid != null
  @Uuid
  @Fetch
  @Unique
  override var uuid: String? = null
  @JsonView(Views.Secure::class)
  override var created: Long? = null
  @JsonView(Views.Secure::class)
  override var lastModified: Long? = null
  
  override fun hashCode(): Int {
    return HashCodeBuilder(13, 39)
      .append(id)
      .append(uuid)
      .toHashCode()
  }

  /**
   * Tests if two entities are equal.
   * This is used in Lists etc. to ensure only single entities.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is FBaseImpl) return false

    if (uuid != null && other.uuid != null) {
      if (uuid != other.uuid) return false
    }
    if (initialId) return false
    if (id != other.id) return false

    if (javaClass != other.javaClass) return false

    return true
  }
}