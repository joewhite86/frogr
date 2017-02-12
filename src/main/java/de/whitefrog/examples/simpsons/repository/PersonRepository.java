package de.whitefrog.examples.simpsons.repository;

import de.whitefrog.examples.simpsons.model.Person;
import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}