package de.whitefrog.neobase.model.relationship

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.neobase.exception.NeobaseRuntimeException
import de.whitefrog.neobase.model.Base
import de.whitefrog.neobase.model.Model
import de.whitefrog.neobase.model.annotation.NotPersistant
import de.whitefrog.neobase.model.annotation.Unique
import de.whitefrog.neobase.model.annotation.Uuid
import de.whitefrog.neobase.rest.Views
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

open class BaseRelationship<From:Model, To:Model>(override val from:From, override val to:To) : Relationship<From, To> {
    @JsonView(Views.Hidden::class)
    override var id = random.nextLong()
        get() {
            return if (initialId) -1 else field
        }
        set(value) {
            field = value
            initialId = false
        }
    @Uuid
    @Unique
    override var uuid: String? = null
    @NotPersistant
    override var type: String? = null
    override var created: Long? = null
    @JsonView(Views.Secure::class)
    private var lastModified: Long? = null
    @JsonView(Views.Hidden::class)
    override var modifiedBy: String? = null

    @NotPersistant
    private var initialId = true
    @NotPersistant
    @JsonIgnore
    override var checkedFields: MutableList<String> = ArrayList()
    @NotPersistant
    @JsonIgnore
    override var fetchedFields: MutableList<String> = ArrayList()

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

    override fun getLastModified(): Long? {
        return lastModified
    }
    override fun updateLastModified() {
        this.lastModified = System.currentTimeMillis()
    }

    override val isPersisted: Boolean
        get() = id > 0 || uuid != null

    override fun <T : Base> clone(vararg fields: String): T {
        return clone(Arrays.asList(*fields))
    }

    override fun <T : Base> clone(fields: List<String>): T {
        val base: T
        try {
            base = javaClass.newInstance() as T
        } catch (e: ReflectiveOperationException) {
            throw NeobaseRuntimeException(e.message, e)
        }

        base.type = type()
        if (fields.isEmpty() || fields.contains(IdProperty) && id > 0) base.id = id
        return base
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Relationship<*, *>) return false

        val base = o as BaseRelationship<*, *>? ?: return false
        
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
        @JvmField val ModifiedBy = "modifiedBy"
        @JvmField val LastModified = "lastModified"
        @JvmField val Created = "created"
        @JvmField val Type = "type"
        @JvmField val Uuid = "uuid"
        private val random = Random()
    }
}
