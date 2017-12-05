package de.whitefrog.froggy.rest.response;

import de.whitefrog.froggy.exception.DuplicateEntryException;
import de.whitefrog.froggy.exception.MissingRequiredException;
import de.whitefrog.froggy.exception.FroggyException;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import java.security.InvalidParameterException;
import java.util.Map;

/**
 * Re-maps exceptions to be more meaningful.
 */
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
  private static final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

  public static final Map<Class<FroggyException>, String> codes = MapUtil.genericMap(
  );

  public javax.ws.rs.core.Response toResponse(Exception exception) {
    Response response = new Response();
    response.setSuccess(false);
    response.setMessage(exception.getMessage());
    if(exception instanceof WebApplicationException) {
      if(!(exception instanceof ForbiddenException) && !(exception instanceof NotAuthorizedException)
        && ((WebApplicationException) exception).getResponse().getStatus() != javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode()) {
        logger.error(exception.getMessage(), exception);
      }
      return javax.ws.rs.core.Response.fromResponse(((WebApplicationException) exception).getResponse())
        .entity(response).build();
    }
    // not severe exceptions, which don't need stack trace logging
    else if(exception instanceof MissingRequiredException || exception instanceof InvalidParameterException ||
            exception instanceof ConstraintViolationException || exception instanceof DuplicateEntryException) {
      logger.error(exception.getMessage());
      return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity(response).build();
    }
    else {
      logger.error(exception.getMessage(), exception);
      return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity(response).build();
    }
  }
}
