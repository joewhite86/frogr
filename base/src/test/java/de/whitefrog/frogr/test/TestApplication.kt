package de.whitefrog.frogr.test

import de.whitefrog.frogr.Application
import de.whitefrog.frogr.rest.request.ServiceInjector
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment

class TestApplication: Application<Configuration> {
  private var serviceInjector: TestServiceInjector? = null
  
  constructor(): super() {
    // register the rest classes
    register("de.whitefrog.frogr.test.rest")
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.test")
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
  }

  override fun getName(): String {
    return "frogr-auth-rest"
  }
}