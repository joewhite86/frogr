package de.whitefrog.auth.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.auth.model.BaseUser;
import de.whitefrog.frogr.auth.repository.BaseUserRepository;

public class UserRepository extends BaseUserRepository<BaseUser> {
  public UserRepository(Service service) {
    super(service);
  }
}
