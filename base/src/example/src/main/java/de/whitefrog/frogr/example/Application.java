package de.whitefrog.frogr.example;

import io.dropwizard.Configuration;

public class Application extends de.whitefrog.frogr.Application<Configuration> {
  public Application() {
    // register the rest classes
    register("de.whitefrog.frogr.example.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.example");
  }
  
  @Override
  public String getName() {
    return "frogr-base-example";
  }

  public static void main(String[] args) throws Exception {
    new Application().run("server", "config/example.yml");
  }
}
