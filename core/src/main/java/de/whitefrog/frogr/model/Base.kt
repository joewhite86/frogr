package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import de.whitefrog.frogr.exception.FrogrException
import java.io.Serializable
import java.util.*
import javax.xml.bind.annotation.XmlRootElement

/**
 * Base interface used for all entity and relationship types.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
interface Base : Serializable {
  /**
   * Unique identifier. Not guaranteed unique.
   * Will be -1 when no id is set yet.
   */
  var id: Long
  /**
   * Already checked fields during a save process. Used to prevent
   * double checking.
   */
  var checkedFields: HashSet<String>
  /**
   * Fields already fetched from database. Used to prevent unneccecary
   * read actions.
   */
  var fetchedFields: HashSet<String>
  var removeProperties: HashSet<String>
  /**
   * True, if the entity was already persisted.
   */
  val isPersisted: Boolean
    @JsonIgnore
    get() = id > -1
  
  
  /**
   * Add a field that already was checked.
   * @param field Field name
   */
  fun addCheckedField(field: String) {
    checkedFields.add(field)
  }

  fun removeProperty(property: String) {
    removeProperties.add(property)
  }

  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  fun <T: Base> clone(vararg fields: String): T {
    return clone(Arrays.asList(*fields))
  }


  /**
   * Create a clone of this entity.
   * @param fields List of fields to clone
   */
  @Suppress("UNCHECKED_CAST")
  fun <T: Base> clone(fields: List<String>): T {
    val base: T
    try {
      base = javaClass.newInstance() as T
      if(id >= 0) base.id = id
    } catch (e: ReflectiveOperationException) {
      throw FrogrException(e.message, e)
    }

    return base
  }

  fun resetId()
  
  companion object {
    const val AllFields = "all"
    const val Id = "id"
  }
}
