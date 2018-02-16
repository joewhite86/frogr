package de.whitefrog.frogr.auth.repository;

import de.whitefrog.frogr.auth.exception.AuthenticationException;
import de.whitefrog.frogr.auth.model.BaseUser;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.rest.oauth.Authenticator;
import de.whitefrog.frogr.exception.MissingRequiredException;
import de.whitefrog.frogr.exception.PersistException;
import de.whitefrog.frogr.model.FieldList;
import de.whitefrog.frogr.model.SaveContext;
import de.whitefrog.frogr.repository.BaseModelRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BaseUserRepository<U extends BaseUser> extends BaseModelRepository<U> {
  private static final Logger logger = LoggerFactory.getLogger(BaseUserRepository.class);
  
  private Role roles = new Role();
  
  public Role getRoles() {
    return roles;
  }

  public String generateHash(String password) {
    return DigestUtils.sha1Hex(password);
  }

  public U login(String login, String password) {
    Validate.notEmpty(login);
    Validate.notEmpty(password);

    Map<String, Object> params = new HashMap<>();
    params.put("login", login);
    params.put("password", generateHash(password));
    Result result = graph().execute(
      "match (m:" + label().name() + ") where m.login = {login} and m.password = {password} return m", params);
    boolean success = result.hasNext();
    try {
      if(success) {
        U user = createModel((Node) Iterators.single(result).get("m"),
          FieldList.parseFields(BaseUser.AccessToken));

        if(user.getAccessToken() == null) {
          user.setAccessToken(UUID.randomUUID().toString());
        }
        Authenticator.updateLastApiAccess(user.getAccessToken());
        save(user);
        return user;
      } else {
        throw new AuthenticationException("Wrong username or password");
      }
    } finally {
      result.close();
    }
  }
  
  public void logout(U user) {
    Authenticator.removeToken(BaseUser.AccessToken);
    user.removeProperty(BaseUser.AccessToken);
    save(user);
  }

  public void register(U user) {
    user.setRole(Role.User);
    save(user);
  }

  @Override
  public void save(SaveContext<U> context) throws PersistException {
    U user = context.model();

    // encrypt password and set defaults for new users
    if(!user.isPersisted()) {
      if(StringUtils.isEmpty(user.getPassword())) throw new MissingRequiredException("must provide a password");
      Set<ConstraintViolation<U>> violations = service().validator().validateProperty(user, BaseUser.Password);
      if(!violations.isEmpty()) {
        for(ConstraintViolation<U> violation : violations) {
          logger.error(violation.getPropertyPath().toString() + " " + violation.getMessage());
        }
        throw new javax.validation.ConstraintViolationException("violations storing " + context.model(), violations);
      }
      user.setPassword(generateHash(user.getPassword()));
    } else {
      user.setPassword(null);
    }

    super.save(context);
  }

  @Override
  public void validateModel(SaveContext<U> context) {
    U user = context.model();
    if(!user.isPersisted()) {
      if(user.getRole() == null) {
        throw new MissingRequiredException("no role specified");
      }
    }
    if(context.fieldChanged(BaseUser.Password)) {
      context.model().addCheckedField(BaseUser.Password);
      super.validateModel(context);
    } else {
      super.validateModel(context);
    }
  }
}
