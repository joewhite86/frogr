package de.whitefrog.froggy.test;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.repository.BaseModelRepository;

public class InvalidRepository extends BaseModelRepository<Person> {
  public InvalidRepository(Service service, String modelName) {
    super(service, modelName);
  }
}
