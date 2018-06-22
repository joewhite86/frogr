package de.whitefrog.frogr.model.relationship

import de.whitefrog.frogr.model.BaseImpl
import de.whitefrog.frogr.model.Model

open class BaseRelationship<From: Model, To: Model>(): BaseImpl(), Relationship<From, To> {
  final override lateinit var from: From
  final override lateinit var to: To

  constructor(from: From, to: To): this() {
    this.from = from
    this.to = to
  }

  override fun toString(): String {
    val typeName = javaClass.simpleName
    return "$typeName [$from -> $to]"
  }
}
