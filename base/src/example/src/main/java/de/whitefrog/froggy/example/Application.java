package de.whitefrog.froggy.example;

import de.whitefrog.froggy.example.model.Person;
import de.whitefrog.froggy.example.repository.PersonRepository;
import io.dropwizard.Configuration;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;

public class Application extends de.whitefrog.froggy.Application<Configuration> {
  public Application() {
    // register the rest classes
    register("de.whitefrog.froggy.example.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.froggy.example");
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
  
  @Override
  public String getName() {
    return "froggy-base-example";
  }

  public static void main(String[] args) throws Exception {
    new Application().run("server", "config/example.yml");
  }
}
