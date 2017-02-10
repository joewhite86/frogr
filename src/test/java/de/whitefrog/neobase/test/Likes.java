package de.whitefrog.neobase.test;

import de.whitefrog.neobase.model.relationship.BaseRelationship;

public class Likes extends BaseRelationship<Person, Person> {
  private String field;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}
