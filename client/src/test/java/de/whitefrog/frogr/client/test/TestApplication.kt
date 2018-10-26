package de.whitefrog.frogr.client.test

import de.whitefrog.frogr.Application
import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.repository.BaseUserRepository
import de.whitefrog.frogr.auth.rest.oauth.Authenticator
import de.whitefrog.frogr.auth.rest.oauth.Authorizer
import de.whitefrog.frogr.client.test.model.User
import de.whitefrog.frogr.rest.request.ServiceInjector
import io.dropwizard.Configuration
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.setup.Environment
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature

fun main(args: Array<String>) {
  val app = TestApplication()
  app.run("server")
  app.serviceInjector().service().beginTx().use { tx ->
    val user = User()
    user.login = "test"
    user.password = "test"
    user.role = Role.User
    app.serviceInjector().service().repository(User::class.java).save(user)
    tx.success()
  }
}

class TestApplication: Application<Configuration> {
  private var serviceInjector: TestServiceInjector? = null
  
  constructor(): super() {
    // register the rest classes
    register("de.whitefrog.frogr.client")
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.client")
  }

  override fun serviceInjector(): ServiceInjector {
    if (serviceInjector == null) {
      serviceInjector = TestServiceInjector()
    }
    return serviceInjector!!
  }

  @Throws(Exception::class)
  override fun run(configuration: Configuration, environment: Environment) {
    super.run(configuration, environment)

    val repository = service().repository<BaseUserRepository<User>, User>(User::class.java)
    val authorizer = Authorizer(repository)
    val oauthFilter = OAuthCredentialAuthFilter.Builder<User>()
      .setAuthenticator(Authenticator(repository))
      .setAuthorizer(authorizer)
      .setPrefix("Bearer")
      .buildAuthFilter()

    environment.jersey().register(RolesAllowedDynamicFeature::class.java)
    environment.jersey().register(AuthValueFactoryProvider.Binder<User>(User::class.java))
    environment.jersey().register(AuthDynamicFeature(oauthFilter))
  }

  override fun getName(): String {
    return "frogr-client-rest"
  }
}