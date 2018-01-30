package de.whitefrog.frogr.auth.example.rest;

import de.whitefrog.frogr.auth.example.model.User;
import de.whitefrog.frogr.auth.example.repository.UserRepository;
import de.whitefrog.frogr.auth.rest.AuthCRUDService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
}
