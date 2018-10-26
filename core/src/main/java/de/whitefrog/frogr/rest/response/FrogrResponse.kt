package de.whitefrog.frogr.rest.response

import org.apache.commons.collections.CollectionUtils
import java.io.Serializable
import java.util.*

/**
 * Model to format responses in a unified way.
 */
class FrogrResponse<T> : Serializable {
  var isSuccess: Boolean = false
  var total: Long? = null
  var message: String? = null
  var errorCode: Int? = null
  var pages: Int? = null
  var data: List<T> = ArrayList()

  fun add(vararg data: T) {
    CollectionUtils.addAll(this.data, data)
  }

  fun singleton(): Any? {
    return if (data.isEmpty()) null else data[0]
  }

  companion object {
    @JvmStatic
    fun <T> build(data: List<T>): FrogrResponse<T> {
      val response = FrogrResponse<T>()
      response.isSuccess = true
      response.data = data
      return response
    }
  }
}
