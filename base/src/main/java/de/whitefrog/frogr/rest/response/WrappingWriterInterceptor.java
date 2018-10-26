package de.whitefrog.frogr.rest.response;

import io.dropwizard.jersey.validation.ValidationErrorMessage;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Response and request filter. Primarily used to format responses. 
 */
@Provider
public class WrappingWriterInterceptor implements ContainerResponseFilter, ContainerRequestFilter {

  @Override
  public void filter(ContainerRequestContext containerRequest) {
//    logger.warn(containerRequest.getUriInfo().getQueryParameters().toString());
  }

  @Override
  public void filter(ContainerRequestContext containerRequest, ContainerResponseContext containerResponse) {
    final Object entity = containerResponse.getEntity();

    if(entity instanceof String || entity instanceof File || entity instanceof StreamingOutput) {
      containerResponse.getHeaders().add("Cache-Control", "no-cache");
      return;
    } else if(entity instanceof FrogrResponse) {
      return;
    }

    final FrogrResponse response = new FrogrResponse();
    if(containerResponse.getStatus() == Response.Status.OK.getStatusCode() ||
       containerResponse.getStatus() == Response.Status.CREATED.getStatusCode() ||
       containerResponse.getStatus() == Response.Status.NO_CONTENT.getStatusCode())
         response.setSuccess(true);
    // Handle JSON responses
    if(entity != null) {
      if(entity instanceof ValidationErrorMessage) {
        response.setMessage(((ValidationErrorMessage) entity).getErrors().get(0));
        response.setSuccess(false);
      } else if(entity instanceof List) {
        response.setData((List) entity);
      } else if(entity instanceof Collection) {
        response.setData(new ArrayList<>((Collection<?>) entity));
      } else {
        response.setData(Collections.singletonList(entity));
      }
    }
    if(containerResponse.getHeaderString("Content-Type") == null) {
      containerResponse.getHeaders().add("Content-Type", "application/json");
    }
    // Tell JAX-RS about new entity.
    containerResponse.setEntity(response);
  }
}
