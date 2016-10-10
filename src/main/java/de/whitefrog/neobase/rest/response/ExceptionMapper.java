package de.whitefrog.neobase.rest.response;

import de.whitefrog.neobase.exception.NeobaseException;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {
  private static final Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

  public static final Map<Class<NeobaseException>, String> codes = MapUtil.genericMap(
  );

  public javax.ws.rs.core.Response toResponse(Exception exception) {
    ModelResponse response = new ModelResponse();
    response.setSuccess(false);
    response.setMessage(exception.getMessage());
    if(exception instanceof WebApplicationException) {
      if(!(exception instanceof ForbiddenException) && !(exception instanceof NotAuthorizedException)
        && ((WebApplicationException) exception).getResponse().getStatus() != javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode()) {
        logger.error(exception.getMessage(), exception);
      }
      return javax.ws.rs.core.Response.fromResponse(((WebApplicationException) exception).getResponse())
        .entity(response).build();
    } else {
      logger.error(exception.getMessage(), exception);
      return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity(response).build();
    }
  }
}
