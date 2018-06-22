package de.whitefrog.frogr.example.oauth.rest;

import de.whitefrog.frogr.example.oauth.model.User;
import de.whitefrog.frogr.example.oauth.repository.UserRepository;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.rest.AuthCRUDService;
import de.whitefrog.frogr.model.SaveContext;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.*;

@Path("user")
public class Users extends AuthCRUDService<UserRepository, User, User> {
  @GET
  @Path("init")
  public String init() {
    String token;
    // insert some data
    try(Transaction tx = service().beginTx()) {
      token = repository().init();
      tx.success();
    }

    return token;
  }

  @POST
  @Path("register")
  public User register(User user) {
    user.setRole(Role.User);
    repository().save(user);
    return user;
  }

  @GET
  @Path("login")
  public User login(@QueryParam("login") String login, @QueryParam("password") String password) {
    try(Transaction tx = service().beginTx()) {
      User user = repository().login(login, password);
      tx.success();
      return user;
    }
  }

  @Override
  public void authorize(User user, User model, SaveContext<User> context) {
    super.authorize(user, model, context);
    if(user != null && !user.equals(model)) throw new ForbiddenException();
  }

  @Override
  public void authorizeDelete(User user, User model) {
    super.authorizeDelete(user, model);
    if(!user.equals(model)) throw new ForbiddenException();
  }
}
