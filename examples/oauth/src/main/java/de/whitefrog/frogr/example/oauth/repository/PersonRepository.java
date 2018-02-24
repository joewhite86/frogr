package de.whitefrog.frogr.example.oauth.repository;

import de.whitefrog.frogr.example.oauth.model.Person;
import de.whitefrog.frogr.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public Person findMorty() {
    return search()
      .filter(Person.Name, "Morty Smith")
      .single();
  }
}
