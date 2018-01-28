package de.whitefrog.froggy.auth.example;

import de.whitefrog.froggy.auth.example.model.User;
import de.whitefrog.froggy.auth.rest.oauth.Authenticator;
import de.whitefrog.froggy.auth.rest.oauth.Authorizer;
import io.dropwizard.Configuration;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class Application extends de.whitefrog.froggy.Application<Configuration> {
  public Application() {
    // register the rest classes
    register("de.whitefrog.froggy.auth.example");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.froggy.auth.example");
    
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
