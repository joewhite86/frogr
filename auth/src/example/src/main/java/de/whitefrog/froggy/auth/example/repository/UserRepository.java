package de.whitefrog.froggy.auth.example.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.auth.example.model.User;
import de.whitefrog.froggy.auth.repository.BaseUserRepository;
import de.whitefrog.froggy.repository.ModelRepository;

public class UserRepository extends BaseUserRepository<User> {
  public UserRepository(Service service) {
    super(service);
  }
}
