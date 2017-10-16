package de.whitefrog.examples.simpsons;

import de.whitefrog.froggy.Application;
import io.dropwizard.Configuration;

public class Simpsons extends Application<Configuration> {
  public Simpsons() {
    register("de.whitefrog.examples.simpsons", "de.whitefrog.examples.simpsons.rest");
  }
  
  public static void main(String[] args) throws Exception {
    new Simpsons().run("server", "config/myband.yml");
  }
}
