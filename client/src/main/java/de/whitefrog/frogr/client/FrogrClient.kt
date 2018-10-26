package de.whitefrog.frogr.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.CaseFormat
import de.whitefrog.frogr.client.exception.ClientException
import de.whitefrog.frogr.client.exception.UnauthorizedException
import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.rest.response.frogrResponse
import de.whitefrog.frogr.model.*
import de.whitefrog.frogr.rest.response.FrogrResponse
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.ws.rs.NotFoundException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class FrogrClient(url: String) {
  private val client = ClientBuilder.newClient()
  private var webTarget = client.target(url)
  private var user: BaseUser? = null
  private var token: String? = null
  private val mapper = jacksonObjectMapper()
  
  private fun getPath(entity: FBase): String {
    val name = if(entity is Model && entity.type != null) entity.type!! else entity.javaClass.simpleName
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name)
  }
  
  private fun buildRequest(path: String, queryParams: Map<String, String> = mapOf()): Invocation.Builder {
    var target = webTarget.path(path)
    queryParams.forEach { name, value -> target = target.queryParam(name, value) }
    
    var builder = target.request(MediaType.APPLICATION_JSON)
    if(token != null) builder = builder.header("Authorization", "Bearer $token")
    
    return builder
  }
  
  @Throws(UnauthorizedException::class, NotFoundException::class, DuplicateEntryException::class, ClientException::class)
  private fun <T: FBase> processError(response: Response, clazz: Class<T>) {
    try {
      processError(response, response.frogrResponse(clazz).message)
    } catch(ex: MessageBodyProviderNotFoundException) {
      processError(response)
    }
  }
  @Throws(UnauthorizedException::class, NotFoundException::class, DuplicateEntryException::class, ClientException::class)
  private fun processError(response: Response, message: String? = null) {
    if(response.status == Response.Status.CONFLICT.statusCode) {
      throw DuplicateEntryException(message)
    }
    if(response.status == Response.Status.NOT_FOUND.statusCode) {
      throw NotFoundException(message)
    }
    if(response.status == Response.Status.UNAUTHORIZED.statusCode) {
      throw UnauthorizedException("not authorized")
    }
    throw ClientException(message)
  }

  fun login(login: String, password: String): BaseUser {
    val response = buildRequest("user/login", mapOf(Pair("login", login), Pair("password", password)))
      .get(object: GenericType<FrogrResponse<BaseUser>>() {})
    user = response.data[0]
    token = response.data[0].accessToken!!

    return response.data[0]
  }
  fun logout(user: BaseUser) {
    val response = buildRequest("user/logout")
      .post(Entity.entity(user, MediaType.APPLICATION_JSON))
    if(response.status != Response.Status.NO_CONTENT.statusCode) {
      processError(response, "could not log out user")
    }
  }
  
  fun <T: FBase> getGenericType(clazz: Class<T>): GenericType<FrogrResponse<T>> {
    return object: GenericType<FrogrResponse<T>>(object: ParameterizedType {
      override fun getActualTypeArguments(): Array<Type> {
        return arrayOf(clazz)
      }
      override fun getRawType(): Type {
        return FrogrResponse::class.java
      }
      override fun getOwnerType(): Type {
        return FrogrResponse::class.java
      }
    }) {}
  }

  @Throws(UnauthorizedException::class, DuplicateEntryException::class, ClientException::class)
  fun <T: FBase> create(vararg entities: T): FrogrResponse<T> {
    val response = buildRequest(getPath(entities.first()))
      .post(Entity.json(entities))

    if(response.status != Response.Status.CREATED.statusCode) {
      processError(response, entities[0].javaClass)
    }

    return response.frogrResponse(entities[0].javaClass)
  }
  @Throws(UnauthorizedException::class, NotFoundException::class, DuplicateEntryException::class, ClientException::class)
  fun <T: FBase> update(vararg entities: T): FrogrResponse<T> {
    val response = buildRequest(getPath(entities.first()))
      .put(Entity.json(entities))
    
    if(response.status != Response.Status.OK.statusCode) {
      processError(response, entities[0].javaClass)
    }

    return response.frogrResponse(entities[0].javaClass)
  }
  fun get(path: String, params: SearchParameter? = null): Response {
    return buildRequest(path)
      .header("params", mapper.writeValueAsString(params))
      .get()
  }
  @Throws(UnauthorizedException::class, ClientException::class)
  fun <T: FBase> search(clazz: Class<T>, params: SearchParameter): FrogrResponse<T> {
    val path = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, clazz.simpleName)
    val response = buildRequest(path)
      .header("params", mapper.writeValueAsString(params))
      .get()

    if(response.status != Response.Status.OK.statusCode) {
      processError(response, FBaseImpl::class.java)
    }

    return response.frogrResponse(clazz)
  }
  @Throws(UnauthorizedException::class, NotFoundException::class, ClientException::class)
  fun <T: FBase> delete(entity: T) {
    val response = buildRequest(getPath(entity) + "/" + entity.uuid)
      .delete()

    if(response.status != Response.Status.OK.statusCode) {
      processError(response, entity.javaClass)
    }
  }
}
