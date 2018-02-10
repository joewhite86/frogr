package de.whitefrog.frogr.auth.test

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.rest.request.ServiceInjector
import de.whitefrog.frogr.test.TemporaryService
import io.dropwizard.testing.ResourceHelpers

class TestServiceInjector : ServiceInjector() {
  private var service: Service = TemporaryService()

  override fun service(): Service {
    return service
  }

  override fun provide(): Service {
    if (!service.isConnected) {
      service.setConfig(ResourceHelpers.resourceFilePath("config/neo4j.properties"))
      service.connect()
    }
    return service
  }

  override fun dispose(service: Service) {
    service.shutdown()
  }
}