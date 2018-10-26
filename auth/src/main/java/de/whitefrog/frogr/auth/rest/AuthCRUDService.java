package de.whitefrog.frogr.auth.rest;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.frogr.model.BaseUser;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.exception.DuplicateEntryException;
import de.whitefrog.frogr.model.FBase;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.repository.Repository;
import de.whitefrog.frogr.rest.Views;
import de.whitefrog.frogr.rest.request.SearchParam;
import de.whitefrog.frogr.rest.response.FrogrResponse;
import de.whitefrog.frogr.rest.service.RestService;
import io.dropwizard.auth.Auth;
import io.dropwizard.validation.Validated;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

abstract public class AuthCRUDService <R extends Repository<M>, M extends Model, U extends BaseUser> extends RestService<R, M> {
  private static final Logger logger = LoggerFactory.getLogger(AuthCRUDService.class);

  @POST
  @RolesAllowed({Role.User})
  @JsonView(Views.Secure.class)
  public Response create(@Auth U user, List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        if(model.isPersisted()) {
          throw new ForbiddenException("the model is not yet persisted");
        }
        SaveContext<M> context = new SaveContext<>(repository(), model);
        authorize(user, model, context);
        try {
          repository().save(context);
        } catch(Exception e) {
          logger.error("failed to save {}", model);
          throw e;
        }
      }

      tx.success();
    } catch(DuplicateEntryException e) {
      return Response.status(Response.Status.CONFLICT).build();
    }
    
    return Response
      .status(Response.Status.CREATED)
      .entity(FrogrResponse.build(models)).build();
  }

  @PUT
  @RolesAllowed({Role.User})
  @JsonView(Views.Secure.class)
  public List<M> update(@Auth U user, List<M> models) {
    try(Transaction tx = service().beginTx()) {
      for(M model : models) {
        if(!model.isPersisted()) {
          throw new ForbiddenException("the model has to be created first");
        }
        if(model instanceof FBase && repository().findByUuid(((FBase) model).getUuid()) == null) {
          throw new NotFoundException("model with uuid \"" + ((FBase) model).getUuid() + "\" could not be found");
        }
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
  @Path("{uuid: [a-zA-Z0-9]+}")
  @RolesAllowed({Role.User})
  @JsonView({Views.Public.class})
  @SuppressWarnings("unchecked")
  public M read(@Auth U user, @PathParam("uuid") String uuid,
                    @SearchParam SearchParameter params) {
    return (M) search(user, params.uuids(uuid)).singleton();
  }

  @GET
  @RolesAllowed({Role.User})
  @JsonView({ Views.Public.class })
  @SuppressWarnings("unchecked")
  public FrogrResponse search(@Auth U user, @SearchParam SearchParameter params) {
    Timer.Context timer = metrics.timer(repository().getType().toLowerCase() + ".search").time();
    FrogrResponse response = new FrogrResponse<>();

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
  @RolesAllowed({Role.User})
  @JsonView({Views.Public.class})
  public FrogrResponse searchPost(@Auth U user, SearchParameter params) {
    return search(user, params);
  }

  @DELETE
  @Path("{uuid: [a-zA-Z0-9]+}")
  @RolesAllowed({Role.User})
  public Response delete(@Auth U user, @PathParam("uuid") String uuid) {
    try(Transaction tx = service().beginTx()) {
      M model = repository().findByUuid(uuid);
      if(model == null) throw new NotFoundException();
      authorizeDelete(user, model);
      repository().remove(model);
      tx.success();
    }
    
    return Response.ok().build();
  }

  @POST
  @Path("authorize")
  public void authorize(@Validated Model model) {}

  /**
   * Called on create and update to verify the user has access to the resource.
   * @param user Authenticated user
   * @param model Model to create or update
   * @param context The created save context
   */
  public void authorize(U user, M model, SaveContext<M> context) {}

  /**
   * Called on delete to verify the user has access to the resource.
   * @param user Authenticated user
   * @param model Model to delete
   */
  public void authorizeDelete(U user, M model) {}
}
