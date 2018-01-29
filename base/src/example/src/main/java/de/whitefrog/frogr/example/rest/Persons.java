package de.whitefrog.frogr.example.rest;

import de.whitefrog.frogr.example.model.Person;
import de.whitefrog.frogr.example.repository.PersonRepository;
import de.whitefrog.frogr.rest.service.CRUDService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("persons")
public class Persons extends CRUDService<PersonRepository, Person> {
  @GET
  @Path("init")
  public void init() {
    // insert some data
    try(Transaction tx = service().beginTx()) {
      if(repository().search().count() == 0) {
        repository().init();
        tx.success();
      }
    }
  }
}
