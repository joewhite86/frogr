package de.whitefrog.froggy.exception;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

public class TypeMismatchException extends RuntimeException {
  Node node;
  Label label;

  public TypeMismatchException(Node node, Label label) {
    super("The expected label \"" + label + "\" was not found on " + node);
    this.node = node;
    this.label = label;
  }
}
