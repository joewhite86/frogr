package de.whitefrog.examples.simpsons;

import org.neo4j.graphdb.RelationshipType;

public abstract class RelationshipTypes {
  public static final String MarriedWith = "MarriedWith";
  public static final String ChildOf = "ChildOf";
  
  public enum t implements RelationshipType {
    MarriedWith, ChildOf
  }
}
