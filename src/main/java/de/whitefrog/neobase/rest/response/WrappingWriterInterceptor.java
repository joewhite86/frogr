package de.whitefrog.neobase.rest.response;

import io.dropwizard.jersey.validation.ValidationErrorMessage;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
      return;
    } else if(entity instanceof Response) {
      return;
    }

    final Response response = new Response();
    response.setSuccess(true);
    // Handle JSON responses
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

    // Tell JAX-RS about new entity.
    containerResponse.setEntity(response);
  }
}
