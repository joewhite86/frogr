package de.whitefrog.frogr.test.repository;

import de.whitefrog.frogr.repository.BaseModelRepository;
import de.whitefrog.frogr.test.model.Person;

public class InvalidRepository extends BaseModelRepository<Person> {
  public InvalidRepository(String modelName) {
    super(modelName);
  }
}
