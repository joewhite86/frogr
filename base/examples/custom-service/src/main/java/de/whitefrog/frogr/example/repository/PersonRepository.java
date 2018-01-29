package de.whitefrog.frogr.example.repository;

import de.whitefrog.frogr.example.model.Person;
import de.whitefrog.frogr.repository.BaseModelRepository;

import java.util.Arrays;

public class PersonRepository extends BaseModelRepository<Person> {
  public void init() {
    Person rick = new Person("Rick Sanchez");
    Person beth = new Person("Beth Smith");
    Person jerry = new Person("Jerry Smith");
    Person morty = new Person("Morty Smith");
    Person summer = new Person("Summer Smith");
    // we need to save the people first, before we can create relationships
    save(rick, beth, jerry, morty, summer);

    rick.setChildren(Arrays.asList(beth));
    beth.setChildren(Arrays.asList(morty, summer));
    beth.setMarriedWith(jerry);
    jerry.setChildren(Arrays.asList(morty, summer));
    jerry.setMarriedWith(beth);
    save(rick, beth, jerry, morty, summer);
  }
}
