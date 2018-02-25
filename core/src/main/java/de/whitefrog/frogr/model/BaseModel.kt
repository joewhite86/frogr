package de.whitefrog.frogr.model

import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.annotation.Fetch

open class BaseModel : BaseImpl(), Model {
  @Fetch
  override var type: String? = null
  
  override fun toString(): String {
    val typeName = type()?: javaClass.simpleName
    return "$typeName ($id)"
  }
  @Suppress("UNCHECKED_CAST")
  override fun <T: Base> clone(fields: List<String>): T {
    val base: BaseModel
    try {
      base = javaClass.newInstance() as BaseModel
      if(id >= 0) base.id = id
      base.type = type()
    } catch (e: ReflectiveOperationException) {
      throw FrogrException(e.message, e)
    }

    return base as T
  }
}