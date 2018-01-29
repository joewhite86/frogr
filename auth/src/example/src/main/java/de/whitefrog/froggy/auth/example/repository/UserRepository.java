package de.whitefrog.frogr.auth.example.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.auth.example.model.User;
import de.whitefrog.frogr.auth.repository.BaseUserRepository;
import de.whitefrog.frogr.repository.ModelRepository;

public class UserRepository extends BaseUserRepository<User> {
  public UserRepository(Service service) {
    super(service);
  }
}
