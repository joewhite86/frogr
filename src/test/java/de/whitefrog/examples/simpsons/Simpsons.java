package de.whitefrog.examples.simpsons;

import de.whitefrog.froggy.Application;
import io.dropwizard.Configuration;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Path("/")
@Singleton
public class Simpsons extends Application<Configuration> {
  public Simpsons() {
    register("de.whitefrog.examples.simpsons");
    serviceInjector().service().register("de.whitefrog");
  }
  
  public static void main(String[] args) throws Exception {
    new Simpsons().run("server", "src/main/resources/config/simpsons.yml");
  }
}
