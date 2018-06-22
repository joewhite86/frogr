package de.whitefrog.frogr.example.basic;

import de.whitefrog.frogr.Application;
import io.dropwizard.Configuration;

public class MyApplication extends Application<Configuration> {
  private MyApplication() {
    // register the rest classes
    register("de.whitefrog.frogr.example.basic.rest");
    // register repositories and models
    serviceInjector().service().register("de.whitefrog.frogr.example.basic");
    // use common config instead of the default config/neo4j.properties
    serviceInjector().service().setConfig("../config/neo4j.properties");
  }
  
  @Override
  public String getName() {
    return "frogr-base-example";
  }

  public static void main(String[] args) throws Exception {
    new MyApplication().run("server", "../config/example.yml");
  }
}
