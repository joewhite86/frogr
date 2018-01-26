package de.whitefrog.auth;

import de.whitefrog.auth.model.BaseUser;
import de.whitefrog.auth.repository.UserRepository;
import de.whitefrog.froggy.Application;
import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.auth.rest.oauth.Authenticator;
import de.whitefrog.froggy.auth.rest.oauth.Authorizer;
import io.dropwizard.Configuration;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Path("/")
@Singleton
public class Simpsons extends Application<Configuration> {
  public Simpsons() {
    register("de.whitefrog.examples.auth");
    serviceInjector().service().register("de.whitefrog");
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    super.run(configuration, environment);

    UserRepository repository = service().repository(BaseUser.class);
    Authorizer authorizer = new Authorizer<>(repository);
    Authenticator authenticator = new Authenticator<>(repository);
    AuthFilter oauthFilter = new OAuthCredentialAuthFilter.Builder<BaseUser>()
      .setAuthenticator(authenticator)
      .setAuthorizer(authorizer)
      .setPrefix("Bearer")
      .buildAuthFilter();

    environment.jersey().register(RolesAllowedDynamicFeature.class);
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(BaseUser.class));
    environment.jersey().register(new AuthDynamicFeature(oauthFilter));

    environment.jersey().register(new AbstractBinder() {
      @Override
      protected void configure() {
        bindFactory(serviceInjector()).to(Service.class);
      }
    });
  }

  public static void main(String[] args) throws Exception {
    new Simpsons().run("server", "src/main/resources/config/simpsons.yml");
  }
}

