package de.whitefrog.frogr.exception

import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node

/**
 * Thrown when a model is passed with the wrong type information.
 */
class TypeMismatchException(internal var node: Node, internal var label: Label) : 
  FrogrException("The expected label \"$label\" was not found on $node")
