package de.whitefrog.neobase.rest.request;

import de.whitefrog.neobase.Service;
import org.glassfish.hk2.api.Factory;

import javax.ws.rs.ext.Provider;

@Provider
public class ServiceInjector implements Factory<Service> {
  private Service service = new Service();

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
