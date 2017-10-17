package de.whitefrog.froggy.exception;

/**
 * Thrown when a repository could not be found.
 */
public class RepositoryNotFoundException extends RepositoryInstantiationException {
  private final String name;

  public RepositoryNotFoundException(String name) {
    super("no matching repository for " + name + " found");
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
