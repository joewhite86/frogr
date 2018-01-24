package de.whitefrog.froggy.example.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.example.model.Person;
import de.whitefrog.froggy.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}
