package de.whitefrog.frogr.exception

/**
 * Thrown when a repository could not be found.
 */
class RepositoryNotFoundException(val name: String) : RepositoryInstantiationException("no matching repository for $name found")
