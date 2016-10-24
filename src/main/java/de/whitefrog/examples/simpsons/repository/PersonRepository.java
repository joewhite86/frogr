package de.whitefrog.examples.simpsons.repository;

import de.whitefrog.examples.simpsons.model.Person;
import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.repository.BaseRepository;

public class PersonRepository extends BaseRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}
