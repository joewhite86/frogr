package de.whitefrog.frogr.example.rest;

import de.whitefrog.frogr.example.model.Person;
import de.whitefrog.frogr.example.repository.PersonRepository;
import de.whitefrog.frogr.model.SearchParameter;
import de.whitefrog.frogr.rest.request.SearchParam;
import de.whitefrog.frogr.rest.service.CRUDService;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

@Path("person")
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
  
  @GET
  @Path("custom-search")
  public List<Person> customSearch(@SearchParam SearchParameter params) {
    try(Transaction ignored = service().beginTx()) {
      return repository().search().params(params).list();
    }
  }
}
