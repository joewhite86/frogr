package de.whitefrog.frogr.rest.response

import de.whitefrog.frogr.model.FBase
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response

fun <T> build(response: Response, clazz: Class<T>): FrogrResponse<T> {
  return response.readEntity<FrogrResponse<T>>(object: GenericType<FrogrResponse<T>>(object: ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> {
      return arrayOf(clazz)
    }
    override fun getRawType(): Type {
      return FrogrResponse::class.java
    }
    override fun getOwnerType(): Type {
      return FrogrResponse::class.java
    }
  }) {})
}


inline fun <T: FBase> Response.frogrResponse(clazz: Class<T>): FrogrResponse<T> = build(this, clazz)