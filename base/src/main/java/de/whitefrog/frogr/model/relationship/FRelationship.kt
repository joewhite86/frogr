package de.whitefrog.frogr.model.relationship

import de.whitefrog.frogr.model.FBase
import de.whitefrog.frogr.model.FBaseImpl
import de.whitefrog.frogr.model.Model

/**
 * Base class for all relationships between entities.
 * Cannot be abstract, because it is used in DefaultRelationshipRepository as default
 */
open class FRelationship<From : Model, To : Model>() : FBaseImpl(), FBase, Relationship<From, To> {
  final override lateinit var from: From
  final override lateinit var to: To
  
  constructor(from: From, to: To): this() {
    this.from = from
    this.to = to
  }
}
