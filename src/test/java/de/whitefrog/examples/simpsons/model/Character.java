package de.whitefrog.examples.simpsons.model;

import de.whitefrog.examples.simpsons.RelationshipTypes;
import de.whitefrog.froggy.model.Entity;
import de.whitefrog.froggy.model.annotation.RelatedTo;
import de.whitefrog.froggy.model.annotation.Unique;
import org.neo4j.graphdb.Direction;

import java.util.List;

public class Character extends Entity {
  @Unique
  private String name;
  @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
  private Character marriedWith;
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  private List<Character> parents;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Character getMarriedWith() {
    return marriedWith;
  }

  public void setMarriedWith(Character marriedWith) {
    this.marriedWith = marriedWith;
  }

  public List<Character> getParents() {
    return parents;
  }

  public void setParents(List<Character> parents) {
    this.parents = parents;
  }
}
