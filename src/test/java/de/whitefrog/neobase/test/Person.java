package de.whitefrog.neobase.test;

import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.annotation.Lazy;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import de.whitefrog.neobase.model.annotation.Unique;
import de.whitefrog.neobase.model.annotation.Uuid;
import org.neo4j.graphdb.Direction;

import java.util.List;

public class Person extends Entity {
  @Uuid
  @Unique
  private String uniqueField;
  private String field;
  @Lazy @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  private List<Person> likes;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getUniqueField() {
    return uniqueField;
  }

  public void setUniqueField(String uniqueField) {
    this.uniqueField = uniqueField;
  }

  public List<Person> getLikes() {
    return likes;
  }

  public void setLikes(List<Person> likes) {
    this.likes = likes;
  }
}
