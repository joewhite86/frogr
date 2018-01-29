package de.whitefrog.frogr.exception;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

/**
 * Thrown when a model is passed with the wrong type information.
 */
public class TypeMismatchException extends FrogrException {
  Node node;
  Label label;

  public TypeMismatchException(Node node, Label label) {
    super("The expected label \"" + label + "\" was not found on " + node);
    this.node = node;
    this.label = label;
  }
}
