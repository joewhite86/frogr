package de.whitefrog.frogr.rest.service;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.repository.Repository;
import de.whitefrog.frogr.rest.Views;
import de.whitefrog.frogr.rest.request.SearchParam;
import de.whitefrog.frogr.rest.response.FrogrResponse;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

public abstract class CRUDService<R extends Repository<M>, M extends Model> extends RestService<R, M> {
  private static final Logger logger = LoggerFactory.getLogger(CRUDService.class);
  @POST
  public Response create(List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        if(model.getPersisted()) {
          throw new ForbiddenException("the model is not yet persisted");
        }
        SaveContext<M> context = new SaveContext<>(repository(), model);
        try {
          repository().save(context);
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
        if(!model.getPersisted()) {
          throw new ForbiddenException("the model has to be created first");
        }
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
  public FrogrResponse search(@SearchParam SearchParameter params) {
    Timer.Context timer = metrics.timer(repository().getModelClass().getSimpleName().toLowerCase() + ".search").time();
    FrogrResponse response = new FrogrResponse<>();

    try(Transaction tx = service().beginTx()) {
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
  public FrogrResponse searchPost(SearchParameter params) {
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
}
