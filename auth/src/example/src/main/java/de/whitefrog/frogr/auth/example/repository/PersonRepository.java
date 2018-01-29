package de.whitefrog.frogr.auth.example.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.auth.example.model.Person;
import de.whitefrog.frogr.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}
