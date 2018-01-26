package de.whitefrog.froggy.rest.service;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.SaveContext;
import de.whitefrog.froggy.model.rest.SearchParameter;
import de.whitefrog.froggy.repository.Repository;
import de.whitefrog.froggy.rest.Views;
import de.whitefrog.froggy.rest.request.SearchParam;
import de.whitefrog.froggy.rest.response.Response;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.util.List;

public abstract class CRUDService<R extends Repository<M>, M extends Model> extends RestService<R, M> {
  private static final Logger logger = LoggerFactory.getLogger(CRUDService.class);
  @POST
  public List<M> create(List<M> models) {
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

    return models;
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
  @Path("{id: [0-9]+}")
  public M read(@PathParam("id") long id,
                @SearchParam SearchParameter params) {
    return (M) search(params.ids(id)).singleton();
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
  public Response search(@SearchParam SearchParameter params) {
    Timer.Context timer = metrics.timer("myband." + repository().getModelClass().getSimpleName().toLowerCase() + ".search").time();
    Response response = new Response<>();

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
  public Response searchPost(SearchParameter params) {
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
