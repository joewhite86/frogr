package de.whitefrog.froggy.auth.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.auth.model.Role;
import de.whitefrog.froggy.auth.model.BaseUser;
import de.whitefrog.froggy.repository.BaseModelRepository;

public abstract class BaseUserRepository<U extends BaseUser> extends BaseModelRepository<U> {
  private Role roles = new Role();
  
  public BaseUserRepository(Service service) {
    super(service);
  }
  
  public Role getRoles() {
    return roles;
  }
}
