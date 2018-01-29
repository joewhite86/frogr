package de.whitefrog.frogr.auth.rest;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.frogr.auth.model.BaseUser;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.rest.SearchParameter;
import de.whitefrog.frogr.rest.Views;
import de.whitefrog.frogr.rest.request.SearchParam;
import de.whitefrog.frogr.rest.response.Response;
import de.whitefrog.frogr.rest.service.DefaultRestService;
import io.dropwizard.auth.Auth;
import io.dropwizard.validation.Validated;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
public abstract class DefaultAuthCRUDService<M extends Model, U extends BaseUser> extends DefaultRestService<M> {
  private static final Logger logger = LoggerFactory.getLogger(DefaultAuthCRUDService.class);

  @POST
  @RolesAllowed({Role.User})
  public List<M> create(@Auth U user, List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        if(model.getPersisted()) {
          throw new WebApplicationException(javax.ws.rs.core.Response.Status.FORBIDDEN);
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

    return models;
  }

  @PUT
  @RolesAllowed({Role.User})
  public List<M> update(@Auth U user, List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        SaveContext<M> context = new SaveContext<>(repository(), model);
        authorize(user, model, context);
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
  @RolesAllowed({Role.User})
  @SuppressWarnings("unchecked")
  public M read(@Auth U user, @PathParam("id") long id,
                @SearchParam SearchParameter params) {
    return (M) search(user, params.ids(id)).singleton();
  }

  @GET
  @Path("{uuid: [a-zA-Z0-9]+}")
  @RolesAllowed({Role.User})
  @JsonView({Views.Public.class})
  public Model read(@Auth U user, @PathParam("uuid") String uuid,
                    @SearchParam SearchParameter params) {
    return (Model) search(user, params.uuids(uuid)).singleton();
  }

  @GET
  @RolesAllowed({Role.User})
  @JsonView({ Views.Public.class })
  public Response<M> search(@Auth U user, @SearchParam SearchParameter params) {
    Timer.Context timer = metrics.timer("myband." + repository().getModelClass().getSimpleName().toLowerCase() + ".search").time();
    Response<M> response = new Response<>();

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
  @RolesAllowed({Role.User})
  @JsonView({Views.Public.class})
  public Response<M> searchPost(@Auth U user, SearchParameter params) {
    return search(user, params);
  }

  @DELETE
  @Path("{uuid: [a-zA-Z0-9]+}")
  @RolesAllowed({Role.User})
  public void delete(@Auth U user, @PathParam("uuid") String uuid) {
    try(Transaction tx = service().beginTx()) {
      M model = repository().findByUuid(uuid);
      if(model == null) throw new NotFoundException();
      authorizeDelete(user, model);
      repository().remove(model);
      tx.success();
    }
  }

  @POST
  @Path("authorize")
  public void authorize(@Validated Model model) {}
  
  public void authorize(U user, M model, SaveContext<M> context) {}

  public void authorizeDelete(U user, M model) {}
}
