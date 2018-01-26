package de.whitefrog.auth.rest;

import de.whitefrog.auth.model.BaseUser;
import de.whitefrog.auth.repository.UserRepository;
import de.whitefrog.froggy.rest.service.RestService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("users")
public class Users extends RestService<UserRepository, BaseUser> {
  @GET
  @Path("register-user")
  public BaseUser register() {
    BaseUser user = new BaseUser();
    user.setLogin("123");
    user.setPassword("123");
    try(Transaction tx = service().beginTx()) {
      service().repository(BaseUser.class).save(user);
      tx.success();
    }
    return user;
  }
}
