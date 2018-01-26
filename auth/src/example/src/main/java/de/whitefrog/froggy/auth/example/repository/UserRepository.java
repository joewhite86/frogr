package de.whitefrog.froggy.auth.example.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.auth.example.model.User;
import de.whitefrog.froggy.auth.repository.BaseUserRepository;

public class UserRepository extends BaseUserRepository<User> {
  public UserRepository(Service service) {
    super(service);
  }
}
