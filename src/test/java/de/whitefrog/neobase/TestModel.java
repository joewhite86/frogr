package de.whitefrog.neobase;

import de.whitefrog.neobase.model.Entity;

class TestModel extends Entity {
  private String field;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}
