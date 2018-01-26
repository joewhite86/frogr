package de.whitefrog.froggy.auth.example;

import de.whitefrog.froggy.auth.example.model.Person;
import de.whitefrog.froggy.auth.example.model.User;
import de.whitefrog.froggy.auth.example.repository.PersonRepository;
import de.whitefrog.froggy.auth.rest.oauth.Authenticator;
import de.whitefrog.froggy.auth.rest.oauth.Authorizer;
import io.dropwizard.Configuration;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;

public class Application extends de.whitefrog.froggy.Application<Configuration> {
  public Application() {
    // register the rest classes
    register("de.whitefrog.froggy.auth.example.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.froggy.auth.example");
    // insert some data
    PersonRepository repository = service().repository(Person.class);
    try(Transaction tx = service().beginTx()) {
      if(repository.search().count() == 0) {
        Person rick = new Person("Rick Sanchez");
        Person beth = new Person("Beth Smith");
        Person jerry = new Person("Jerry Smith");
        Person morty = new Person("Morty Smith");
        Person summer = new Person("Summer Smith");
        // we need to save the people first, before we can create relationships
        repository.save(rick, beth, jerry, morty, summer);
        
        rick.setChildren(Arrays.asList(beth));
        beth.setChildren(Arrays.asList(morty, summer));
        beth.setMarriedWith(jerry);
        jerry.setChildren(Arrays.asList(morty, summer));
        jerry.setMarriedWith(beth);
        repository.save(rick, beth, jerry, morty, summer);
      }
      tx.success();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run(Configuration configuration, Environment environment) throws Exception {
    super.run(configuration, environment);

    Authorizer authorizer = new Authorizer(service().repository(User.class));
    AuthFilter oauthFilter = new OAuthCredentialAuthFilter.Builder<User>()
      .setAuthenticator(new Authenticator(service().repository(User.class)))
      .setAuthorizer(authorizer)
      .setPrefix("Bearer")
      .buildAuthFilter();

    environment.jersey().register(RolesAllowedDynamicFeature.class);
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
    environment.jersey().register(new AuthDynamicFeature(oauthFilter));
  }

  @Override
  public String getName() {
    return "froggy-auth-example-rest";
  }

  public static void main(String[] args) throws Exception {
    new Application().run("server", "config/example.yml");
  }
}
