package de.whitefrog.neobase.test;

import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.annotation.Lazy;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import org.neo4j.graphdb.Direction;

import java.util.List;

public class Person extends Entity {
  private String field;
  @Lazy @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  private List<Person> likes;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public List<Person> getLikes() {
    return likes;
  }

  public void setLikes(List<Person> likes) {
    this.likes = likes;
  }
}
