package de.whitefrog.frogr.exception

/**
 * Thrown when something went wrong during persistence.
 * Should be overwritten.
 */
open class PersistException : FrogrException {
  constructor(message: String?) : super(message)
  constructor(cause: Throwable) : super(cause)
  constructor(message: String?, cause: Throwable) : super(message, cause)
}
