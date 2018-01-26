package de.whitefrog.froggy.auth.rest.oauth;

import de.whitefrog.froggy.auth.repository.BaseUserRepository;
import de.whitefrog.froggy.auth.model.BaseUser;
import org.neo4j.graphdb.Transaction;

public class Authorizer<U extends BaseUser> implements io.dropwizard.auth.Authorizer<U> {
  private final BaseUserRepository<U> repository;

  public Authorizer(BaseUserRepository<U> repository) {
    this.repository = repository;
  }

  @Override
  public boolean authorize(U user, String role) {
    try(Transaction tx = repository.service().beginTx()) {
      return user != null && repository.getRoles().inRole(user.getRole(), role);
    }
  }
}
