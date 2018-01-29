package de.whitefrog.frogr.rest.request;

import de.whitefrog.frogr.Service;
import org.glassfish.hk2.api.Factory;

import javax.ws.rs.ext.Provider;

@Provider
public class ServiceInjector implements Factory<Service> {
  private Service service;
  
  public ServiceInjector() {
    service = new Service();
  }
  
  public Service service() {
    return service;
  }

  @Override
  public Service provide() {
    if(!service.isConnected()) service.connect();
    return service;
  }

  @Override
  public void dispose(Service service) {
      service.shutdown();
  }
}
