package de.whitefrog.frogr.health;

import com.codahale.metrics.health.HealthCheck;
import de.whitefrog.frogr.Service;

public class GraphHealthCheck extends HealthCheck {
  private Service service;
  
  public GraphHealthCheck(Service service) {
    this.service = service;
  }
  
  @Override
  protected Result check() {
    if(service.graph() == null) {
      return Result.unhealthy("The service is not connected to a graph");
    }
    return Result.healthy();
  }
}
