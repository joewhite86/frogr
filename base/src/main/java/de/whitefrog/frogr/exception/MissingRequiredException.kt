package de.whitefrog.frogr.exception

import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.relationship.Relationship
import java.lang.reflect.Field

/**
 * Thrown when a required field is missing on a model during persistence.
 */
class MissingRequiredException : PersistException {
  var model: Base? = null
  var relationship: Relationship<*, *>? = null
  val field: Field?

  constructor(msg: String) : super(msg) {
    this.model = null
    this.field = null
  }

  constructor(model: Base, field: Field) : super("The value for the field \"" + field.name + "\" is missing on " + model) {
    this.model = model
    this.field = field
  }

  constructor(model: Relationship<*, *>, field: Field) : super("The value for the field \"" + field.name + "\" is missing on " + model) {
    this.relationship = model
    this.field = field
  }

  constructor(model: Base, field: String) : super("The value for the field \"$field\" is missing on $model") {
    this.model = model
    this.field = null
  }
}
