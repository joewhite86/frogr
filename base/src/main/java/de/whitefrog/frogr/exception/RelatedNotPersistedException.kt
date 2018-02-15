package de.whitefrog.frogr.exception

/**
 * Thrown when a relationship should get persisted when a related model is not persisted yet.
 */
class RelatedNotPersistedException : PersistException {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
}
