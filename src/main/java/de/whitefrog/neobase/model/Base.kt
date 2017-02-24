package de.whitefrog.neobase.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.whitefrog.neobase.model.annotation.Fetch

import javax.xml.bind.annotation.XmlRootElement
import java.io.Serializable


@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
interface Base : Serializable {
    var id: Long
    var uuid: String?
    var type: String?
    @get:JsonIgnore
    var checkedFields: MutableList<String>
    @get:JsonIgnore
    var fetchedFields: MutableList<String>
    var modifiedBy: String?
    @get:JsonIgnore
    val isPersisted: Boolean
    var created: Long?
    
    fun addCheckedField(field: String)

    fun <T : Base> clone(vararg fields: String): T
    fun <T : Base> clone(fields: List<String>): T

    fun type(): String?
    
    fun resetId()
    
    fun getLastModified(): Long?
    fun updateLastModified()

    companion object {
        val AllFields = "all"
        val IdProperty = "id"
        val ModifiedBy = "modifiedBy"
        val LastModified = "lastModified"
        val Created = "created"
        val Type = "type"
        val Uuid = "uuid"
    }
}
