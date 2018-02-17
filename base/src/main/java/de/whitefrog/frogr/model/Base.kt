package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
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
  var id: Long
  var type: String?
  var checkedFields: HashSet<String>
  var fetchedFields: HashSet<String>
  var removeProperties: HashSet<String>
  val isPersisted: Boolean

  fun addCheckedField(field: String)
  fun removeProperty(property: String)

  fun <T : Base> clone(vararg fields: String): T
  fun <T : Base> clone(fields: List<String>): T

  fun type(): String?
  fun resetId()
  
  companion object {
    const val AllFields = "all"
    const val Id = "id"
    const val Type = "type"
  }
}
