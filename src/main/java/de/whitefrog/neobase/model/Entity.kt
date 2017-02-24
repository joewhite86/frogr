package de.whitefrog.neobase.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.neobase.exception.NeobaseRuntimeException
import de.whitefrog.neobase.model.annotation.*
import de.whitefrog.neobase.persistence.FieldDescriptor
import de.whitefrog.neobase.persistence.Persistence
import de.whitefrog.neobase.rest.Views
import org.apache.commons.lang3.builder.HashCodeBuilder

import java.util.ArrayList
import java.util.Arrays
import java.util.Random

abstract class Entity : Model, Comparable<Base> {
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
    @Fetch
    @Unique
    @Indexed
    override var uuid: String? = null
    override var type: String? = null
    @JsonView(Views.Hidden::class)
    override var model: String? = null
    @JsonView(Views.Secure::class)
    override var created: Long? = null
    @JsonView(Views.Secure::class)
    private var lastModified: Long? = null
        private set(value) {
            field = value
        }
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

    override fun <T : Base> clone(vararg fields: String): T {
        return clone(Arrays.asList(*fields))
    }

    override fun <T : Base> clone(fields: List<String>): T {
        val base: T
        try {
            base = javaClass.newInstance() as T
            base.type = type()
            base.id = id
            base.uuid = uuid
            for (descriptor in Persistence.cache().fieldMap(javaClass)) {
                if (fields.contains(descriptor.name) || descriptor.annotations().relatedTo == null && fields.contains(Base.AllFields)) {
                    descriptor.field().isAccessible = true
                    descriptor.field().set(base, descriptor.field().get(this))
                }
            }
        } catch (e: ReflectiveOperationException) {
            throw NeobaseRuntimeException(e.message, e)
        }

        return base
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
    fun setLastModified(lastModified: Long) {
        this.lastModified = lastModified
    }

    override fun updateLastModified() {
        this.lastModified = System.currentTimeMillis()
    }

    override fun resetId() {
        id = random.nextLong()
        initialId = true
    }

    override val isPersisted: Boolean
        get() = id > 0 || uuid != null

    override fun compareTo(o: Base): Int {
        return java.lang.Long.compare(id, o.id)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Entity) return false

        val base = o

        if (uuid != null && base.uuid != null) {
            if (uuid != base.uuid) return false
        } 
        if (initialId) return false
        if (id != base.id) return false

        if (javaClass != base.javaClass) return false

        return true
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode()
    }

    override fun toString(): String {
        val typeName = if (type() != null) type() else javaClass.simpleName
        return "$typeName ($id)"
    }

    companion object {
        private val random = Random()
    }
}
