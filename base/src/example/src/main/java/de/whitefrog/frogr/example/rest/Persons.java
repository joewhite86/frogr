package de.whitefrog.frogr.example.rest;

import de.whitefrog.frogr.example.model.Person;
import de.whitefrog.frogr.example.repository.PersonRepository;
import de.whitefrog.frogr.rest.service.CRUDService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Arrays;

@Path("persons")
public class Persons extends CRUDService<PersonRepository, Person> {
  @GET
  @Path("init")
  public void init() {
    // insert some data
    PersonRepository repository = service().repository(Person.class);
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
      }
      tx.success();
    }
  }
}
