package de.whitefrog.frogr.auth.example.rest;

import de.whitefrog.frogr.auth.example.model.Person;
import de.whitefrog.frogr.auth.example.model.User;
import de.whitefrog.frogr.auth.example.repository.PersonRepository;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.rest.AuthCRUDService;
import io.dropwizard.auth.Auth;
import org.neo4j.graphdb.Transaction;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("person")
public class Persons extends AuthCRUDService<PersonRepository, Person, User> {
  @GET
  @Path("find-morty")
  @RolesAllowed(Role.User)
  public Person findMorty(@Auth User user) {
    try(Transaction ignored = service().beginTx()) {
      return repository().findMorty();
    }
  }
}
