package de.whitefrog.examples.simpsons.model;

import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.annotation.Unique;

public class Person extends Entity {
  @Unique
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
