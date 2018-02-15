package de.whitefrog.frogr.exception

import de.whitefrog.frogr.model.Base

import java.lang.reflect.Field

/**
 * Thrown when a duplicate entry would be generated during persistence.
 */
class DuplicateEntryException : PersistException {
  var bean: Base? = null
  var field: Field? = null

  constructor(message: String?, bean: Base) : super(message) {
    this.bean = bean
  }

  constructor(message: String?, bean: Base, field: Field) : super(message) {
    this.bean = bean
    this.field = field
  }
}
