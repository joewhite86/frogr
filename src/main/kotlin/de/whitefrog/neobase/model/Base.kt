package de.whitefrog.neobase.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable
import javax.xml.bind.annotation.XmlRootElement


@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
interface Base : Serializable {
    var id: Long
    var uuid: String?
    var type: String?
    var checkedFields: MutableList<String>
    var fetchedFields: MutableList<String>
    var modifiedBy: String?
    val isPersisted: Boolean
    var created: Long?
    
    fun addCheckedField(field: String)

    fun <T : Base> clone(vararg fields: String): T
    fun <T : Base> clone(fields: List<String>): T

    fun type(): String?
    
    fun resetId()
    
    fun getLastModified(): Long?
    fun updateLastModified()
}
