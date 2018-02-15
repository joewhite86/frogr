package de.whitefrog.frogr.health

import com.codahale.metrics.health.HealthCheck
import de.whitefrog.frogr.Service

class GraphHealthCheck(private val service: Service) : HealthCheck() {
  public override fun check(): HealthCheck.Result {
    return if (service.graph() == null) {
      HealthCheck.Result.unhealthy("The service is not connected to a graph")
    } else HealthCheck.Result.healthy()
  }
}
