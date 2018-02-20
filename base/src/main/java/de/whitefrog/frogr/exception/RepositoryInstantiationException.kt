package de.whitefrog.frogr.exception

/**
 * Thrown when a repository failed to initialize.
 * This can happen when a wrong model name is passed or
 * the packages are not properly registered in the service.
 */
open class RepositoryInstantiationException : FrogrException {
  constructor(message: String) : super(message)
  constructor(cause: Throwable) : super(cause)
  constructor(s: String, cause: Throwable) : super(s, cause)
}
