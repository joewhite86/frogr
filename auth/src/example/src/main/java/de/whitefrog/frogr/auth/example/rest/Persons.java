package de.whitefrog.frogr.auth.example.rest;

import de.whitefrog.frogr.auth.example.model.Person;
import de.whitefrog.frogr.auth.example.model.User;
import de.whitefrog.frogr.auth.example.repository.PersonRepository;
import de.whitefrog.frogr.auth.example.repository.UserRepository;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.rest.AuthCRUDService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Arrays;

@Path("persons")
public class Persons extends AuthCRUDService<PersonRepository, Person, User> {
  @GET
  @Path("init")
  public String init() {
    String token = "";
    // insert some data
    PersonRepository repository = service().repository(Person.class);
    UserRepository users = service().repository(User.class);
    try(Transaction tx = service().beginTx()) {
      if(repository.search().count() == 0) {
        Person rick = new Person("Rick Sanchez");
        Person beth = new Person("Beth Smith");
        Person jerry = new Person("Jerry Smith");
        Person morty = new Person("Morty Smith");
        Person summer = new Person("Summer Smith");
        // we need to save the people first, before we can create relationships
        repository.save(rick, beth, jerry, morty, summer);

        rick.setChildren(Arrays.asList(beth));
        beth.setChildren(Arrays.asList(morty, summer));
        beth.setMarriedWith(jerry);
        jerry.setChildren(Arrays.asList(morty, summer));
        jerry.setMarriedWith(beth);
        repository.save(rick, beth, jerry, morty, summer);

        User user = new User();
        user.setLogin("justin_roiland");
        user.setPassword("rickandmorty");
        user.setRole(Role.Admin);
        users.save(user);
        user = users.login("justin_roiland", "rickandmorty");
        token = "Access Token: " + user.getAccessToken();
        System.out.println("User created. Authorization: Bearer " + user.getAccessToken());
      } else {
        User user = users.login("justin_roiland", "rickandmorty");
        token = "Access Token: " + user.getAccessToken();
        System.out.println("Authorization: Bearer " + user.getAccessToken());
      }
      tx.success();
    }
    
    return token;
  }
}
