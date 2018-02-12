package de.whitefrog.frogr.test

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.rest.request.ServiceInjector
import de.whitefrog.frogr.test.TemporaryService

class TestServiceInjector : ServiceInjector() {
  private var service: Service = TemporaryService()

  override fun service(): Service {
    return service
  }

  override fun provide(): Service {
    if (!service.isConnected) service.connect()
    return service
  }

  override fun dispose(service: Service) {
    service.shutdown()
  }
}