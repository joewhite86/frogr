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
  var uuid: String?
  var type: String?
  var checkedFields: HashSet<String>
  var fetchedFields: HashSet<String>
  var removeProperties: HashSet<String>
  val persisted: Boolean
  var created: Long?

  fun addCheckedField(field: String)
  fun removeProperty(property: String)

  fun <T : Base> clone(vararg fields: String): T
  fun <T : Base> clone(fields: List<String>): T

  fun type(): String?

  fun resetId()

  fun getLastModified(): Long?
  fun updateLastModified()

  companion object {
    const val AllFields = "all"
    const val IdProperty = "id"
    const val LastModified = "lastModified"
    const val Created = "created"
    const val Type = "type"
    const val Uuid = "uuid"
  }
}
