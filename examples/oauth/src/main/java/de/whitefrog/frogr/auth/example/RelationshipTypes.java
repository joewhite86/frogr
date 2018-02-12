package de.whitefrog.frogr.auth.example;

import org.neo4j.graphdb.RelationshipType;

public abstract class RelationshipTypes {
  public static final String ChildOf = "ChildOf";
  public static final String MarriedWith = "MarriedWith";
  public static final String FriendWith = "FriendWith";

  public enum t implements RelationshipType {
    ChildOf, MarriedWith, FriendWith
  }
}
