package de.whitefrog.frogr.rest.service;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.rest.Views;
import de.whitefrog.frogr.rest.request.SearchParam;
import de.whitefrog.frogr.rest.response.FrogrResponse;
import io.dropwizard.validation.Validated;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
public abstract class DefaultCRUDService<M extends Model> extends DefaultRestService<M> {
  private static final Logger logger = LoggerFactory.getLogger(DefaultCRUDService.class);

  @POST
  public Response create(List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        if(model.isPersisted()) {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
          repository().save(model);
        } catch(Exception e) {
          logger.error("failed to save {}", model);
          throw e;
        }
      }

      tx.success();
    }

    return Response
      .status(Response.Status.CREATED)
      .entity(FrogrResponse.build(models)).build();
  }

  @PUT
  public List<M> update(List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        SaveContext<M> context = new SaveContext<>(repository(), model);
        try {
          repository().save(context);
        } catch(Exception e) {
          logger.error("failed to update {}", model);
          throw e;
        }
      }

      tx.success();
    }

    return models;
  }

  @GET
  @Path("{uuid: [a-zA-Z0-9]+}")
  @JsonView({Views.Public.class})
  public Model read(@PathParam("uuid") String uuid,
                    @SearchParam SearchParameter params) {
    return (Model) search(params.uuids(uuid)).singleton();
  }

  @GET
  @JsonView({ Views.Public.class })
  public FrogrResponse<M> search(@SearchParam SearchParameter params) {
    Timer.Context timer = metrics.timer(repository().getModelClass().getSimpleName().toLowerCase() + ".search").time();
    FrogrResponse<M> response = new FrogrResponse<>();

    try(Transaction ignored = service().beginTx()) {
      SearchParameter paramsClone = params.clone();
      if(params.limit() > 0) {
        List<M> list = repository().search().params(params).list();
        response.setData(list);
      }
      timer.stop();
      response.setSuccess(true);
      if(params.count()) {
        response.setTotal(repository().search().params(paramsClone).count());
      }
    }

    return response;
  }
  
  @POST
  @Path("search")
  @JsonView({Views.Public.class})
  public FrogrResponse<M> searchPost(SearchParameter params) {
    return search(params);
  }

  @DELETE
  @Path("{uuid: [a-zA-Z0-9]+}")
  public void delete(@PathParam("uuid") String uuid) {
    try(Transaction tx = service().beginTx()) {
      M model = repository().findByUuid(uuid);
      if(model == null) throw new NotFoundException();
      repository().remove(model);
      tx.success();
    }
  }

  @POST
  @Path("authorize")
  public void authorize(@Validated Model model) {}
}
