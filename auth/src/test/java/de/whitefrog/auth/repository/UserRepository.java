package de.whitefrog.auth.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.auth.model.BaseUser;
import de.whitefrog.froggy.auth.repository.BaseUserRepository;

public class UserRepository extends BaseUserRepository<BaseUser> {
  public UserRepository(Service service) {
    super(service);
  }
}
