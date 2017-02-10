package de.whitefrog.neobase.test;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}
