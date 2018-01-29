package de.whitefrog.frogr.test;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.repository.BaseModelRepository;

public class InvalidRepository extends BaseModelRepository<Person> {
  public InvalidRepository(Service service, String modelName) {
    super(service, modelName);
  }
}
