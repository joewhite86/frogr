package de.whitefrog.frogr.rest.response

import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.ws.rs.BadRequestException
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response.Status

class TestExceptionMapper {
  private val mapper = ExceptionMapper()
  @Test
  fun mapForbidden() {
    val response = mapper.toResponse(ForbiddenException())
    assertThat(response.status).isEqualTo(Status.FORBIDDEN.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(403)
  }
  @Test
  fun mapNotAuthorized() {
    val response = mapper.toResponse(NotAuthorizedException(javax.ws.rs.core.Response.noContent()))
    assertThat(response.status).isEqualTo(Status.UNAUTHORIZED.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(401)
  }
  @Test
  fun mapBadRequest() {
    val response = mapper.toResponse(BadRequestException())
    assertThat(response.status).isEqualTo(Status.BAD_REQUEST.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(400)
  }
  @Test
  fun mapNotFound() {
    val response = mapper.toResponse(NotFoundException())
    assertThat(response.status).isEqualTo(Status.NOT_FOUND.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(404)
  }
  @Test
  fun mapMissingRequired() {
    val person = Person("test")
    val response = mapper.toResponse(MissingRequiredException(person, "requiredField"))
    assertThat(response.status).isEqualTo(Status.BAD_REQUEST.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(601)
  }
  @Test
  fun mapDuplicateEntry() {
    val person = Person("test")
    val response = mapper.toResponse(DuplicateEntryException("testmsg", person))
    assertThat(response.status).isEqualTo(Status.BAD_REQUEST.statusCode)
    assertThat(response.entity).isInstanceOf(FrogrResponse::class.java)
    val entity = response.entity as FrogrResponse<*>
    assertThat(entity.isSuccess).isFalse()
    assertThat(entity.errorCode).isEqualTo(602)
  }
}