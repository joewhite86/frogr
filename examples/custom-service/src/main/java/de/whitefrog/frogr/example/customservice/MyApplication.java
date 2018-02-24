package de.whitefrog.frogr.example.customservice;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import de.whitefrog.frogr.Application;
import de.whitefrog.frogr.example.customservice.rest.request.MyServiceInjector;
import de.whitefrog.frogr.rest.request.ServiceInjector;
import de.whitefrog.frogr.rest.service.RestService;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application<Configuration> {
  private MyServiceInjector serviceInjector;
  
  private MyApplication() {
    // register the rest classes
    register("de.whitefrog.frogr.example.customservice.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.example.customservice");
  }

  // override to pass our own ServiceInjector implementation
  @Override
  public ServiceInjector serviceInjector() {
    if(serviceInjector == null) {
      serviceInjector = new MyServiceInjector();
    }
    return serviceInjector;
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    super.run(configuration, environment);

    // bind the custom ServiceInjector to our Service implementation, described below
    environment.jersey().register(new AbstractBinder() {
      @Override
      protected void configure() {
        bindFactory(serviceInjector()).to(MyService.class);
      }
    });

    // register metrics
    environment.jersey().register(new InstrumentedResourceMethodApplicationListener(RestService.metrics));

    // add a console reporter for the metrics
    final ConsoleReporter reporter = ConsoleReporter.forRegistry(RestService.metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    // report every 30 minutes
    reporter.start(30, TimeUnit.MINUTES);

    // add a logger reporter for the metrics
    final Slf4jReporter slf4j = Slf4jReporter.forRegistry(RestService.metrics)
      .outputTo(LoggerFactory.getLogger("com.example.metrics"))
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    // report every 30 minutes
    slf4j.start(30, TimeUnit.MINUTES);
  }

  @Override
  public String getName() {
    return "example-rest";
  }

  public static void main(String[] args) throws Exception {
    new MyApplication().run("server", "config/example.yml");
  }
}
