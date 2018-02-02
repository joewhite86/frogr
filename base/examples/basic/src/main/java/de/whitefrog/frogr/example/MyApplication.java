package de.whitefrog.frogr.example;

import de.whitefrog.frogr.Application;
import io.dropwizard.Configuration;

public class MyApplication extends Application<Configuration> {
  public MyApplication() {
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
    new MyApplication().run("server", "config/example.yml");
  }
}
