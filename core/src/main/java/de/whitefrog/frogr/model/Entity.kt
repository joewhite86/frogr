package de.whitefrog.frogr.model

import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.annotation.Fetch

abstract class Entity : FBaseImpl(), FBase, Model {
  @Fetch
  override var type: String? = null
  
  @Suppress("UNCHECKED_CAST")
  override fun <T: Base> clone(fields: List<String>): T {
    val base: Entity
    try {
      base = javaClass.newInstance() as Entity
      if(id >= 0) base.id = id
      base.type = type()
    } catch (e: ReflectiveOperationException) {
      throw FrogrException(e.message, e)
    }

    return base as T
  }

  override fun toString(): String {
    val typeName = type()?: javaClass.simpleName
    val id = if (id == -1L) uuid else id.toString()
    return "$typeName ($id)"
  }
}