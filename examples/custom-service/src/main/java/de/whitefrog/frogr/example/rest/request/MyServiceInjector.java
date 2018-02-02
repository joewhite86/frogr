package de.whitefrog.frogr.example.rest.request;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.example.MyService;
import de.whitefrog.frogr.rest.request.ServiceInjector;

public class MyServiceInjector extends ServiceInjector {
  private MyService service;

  public MyServiceInjector() {
    service = new MyService();
  }

  @Override
  public Service service() {
    return service;
  }

  @Override
  public Service provide() {
    if(!service.isConnected()) service.connect();
    return service;
  }

  @Override
  public void dispose(de.whitefrog.frogr.Service service) {
    service.shutdown();
  }
}