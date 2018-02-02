package de.whitefrog.frogr.test;

import de.whitefrog.frogr.repository.BaseModelRepository;

public class InvalidRepository extends BaseModelRepository<Person> {
  public InvalidRepository(String modelName) {
    super(modelName);
  }
}
