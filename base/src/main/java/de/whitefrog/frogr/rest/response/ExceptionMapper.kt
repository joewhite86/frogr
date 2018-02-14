package de.whitefrog.frogr.rest.response

import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.exception.PersistException
import org.slf4j.LoggerFactory
import java.security.InvalidParameterException
import javax.validation.ConstraintViolationException
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.WebApplicationException
import javax.ws.rs.ext.Provider

/**
 * Re-maps exceptions to be more meaningful.
 */
@Provider
class ExceptionMapper : javax.ws.rs.ext.ExceptionMapper<Exception> {
  companion object {
    private val logger = LoggerFactory.getLogger(ExceptionMapper::class.java)

    private val codes = linkedMapOf(
      Pair(DuplicateEntryException::class.java, 602),
      Pair(PersistException::class.java, 601),
      Pair(FrogrException::class.java, 600)
    )
  }

  override fun toResponse(exception: Exception): javax.ws.rs.core.Response {
    val response = FrogrResponse<Any>()
    response.isSuccess = false
    response.message = exception.message
    if (exception is WebApplicationException) {
      if (exception !is ForbiddenException && exception !is NotAuthorizedException
        && exception.response.status != javax.ws.rs.core.Response.Status.FORBIDDEN.statusCode) {
        logger.error(exception.message, exception)
      }
      response.errorCode = exception.response.status
      return javax.ws.rs.core.Response.fromResponse(exception.response)
        .status(exception.response.status)
        .entity(response).build()
    } else if (exception is MissingRequiredException || exception is InvalidParameterException ||
      exception is ConstraintViolationException || exception is DuplicateEntryException) {
      logger.error(exception.message)
    } else {
      logger.error(exception.message, exception)
    }// not severe exceptions, which don't need stack trace logging
    if (exception is FrogrException) {
      val code = getErrorCode(exception)
      if (code > -1) response.errorCode = code
    }
    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
      .entity(response).build()
  }

  private fun getErrorCode(e: FrogrException): Int {
    for (clazz in codes.keys) {
      if (clazz.isAssignableFrom(e.javaClass)) return codes[clazz]!!
    }
    return -1
  }
}
