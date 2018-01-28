package de.whitefrog.froggy.example;

import io.dropwizard.Configuration;

public class Application extends de.whitefrog.froggy.Application<Configuration> {
  public Application() {
    // register the rest classes
    register("de.whitefrog.froggy.example.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.froggy.example");
  }
  
  @Override
  public String getName() {
    return "froggy-base-example";
  }

  public static void main(String[] args) throws Exception {
    new Application().run("server", "config/example.yml");
  }
}
