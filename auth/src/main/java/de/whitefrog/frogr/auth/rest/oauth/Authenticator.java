package de.whitefrog.frogr.auth.rest.oauth;

import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.repository.BaseUserRepository;
import de.whitefrog.frogr.auth.model.BaseUser;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Authenticator<U extends BaseUser> implements io.dropwizard.auth.Authenticator<String, U> {
  private static final boolean SimulateUnauthorized = false;
  public static final int ACCESS_TOKEN_EXPIRE_TIME_MIN = 30;
  private BaseUserRepository<U> repository;
  private static Map<String, Long> lastApiAccess = new HashMap<>(100000);

  public Authenticator(BaseUserRepository<U> repository) {
    this.repository = repository;
  }
  
  public static void updateLastApiAccess(String accessToken) {
    lastApiAccess.put(accessToken, System.currentTimeMillis());
  }

  @Override
  public Optional<U> authenticate(String accessToken) {
    if(SimulateUnauthorized) {
      return Optional.empty();
    }
    // Check input, must be a valid UUID
    if(accessToken.equals("public")) {
      U pub = repository.createModel();
      pub.setRole(Role.Public);
      return Optional.of(pub);
    }
    try {
      UUID.fromString(accessToken);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }

    // Get the access token from the database
    try(Transaction tx = repository.service().beginTx()) {
      U user = repository.search()
        .filter(BaseUser.AccessToken, accessToken)
        .fields(BaseUser.Roles)
        .single();
      Optional<U> userOptional;
      if(user != null) {
        userOptional = Optional.of(user);
      } else {
        return Optional.empty();
      }

      if(!lastApiAccess.containsKey(accessToken)) {
        return Optional.empty();
      } else {
        // Check if the last access time is not too far in the past (the access token is expired)
        long lastLogin = lastApiAccess.get(accessToken);
        if(System.currentTimeMillis() - lastLogin > (ACCESS_TOKEN_EXPIRE_TIME_MIN * 60 * 1000)) {
          return Optional.empty();
        }
        // Update the access time for the token
        lastApiAccess.put(accessToken, System.currentTimeMillis());
      }
      
      tx.success();
      // Return the user
      return userOptional;
    }
  }
}
