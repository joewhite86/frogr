package de.whitefrog.frogr.example.oauth;

import de.whitefrog.frogr.Application;
import de.whitefrog.frogr.example.oauth.model.User;
import de.whitefrog.frogr.auth.rest.oauth.Authenticator;
import de.whitefrog.frogr.auth.rest.oauth.Authorizer;
import io.dropwizard.Configuration;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class MyApplication extends Application<Configuration> {
  private MyApplication() {
    // register the rest classes
    register("de.whitefrog.frogr.example.oauth");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.example.oauth");
    
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
    return "frogr-auth-example-rest";
  }

  public static void main(String[] args) throws Exception {
    new MyApplication().run("server", "config/example.yml");
  }
}
