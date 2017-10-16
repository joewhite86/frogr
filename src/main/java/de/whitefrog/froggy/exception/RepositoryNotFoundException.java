package de.whitefrog.froggy.exception;

public class RepositoryNotFoundException extends RuntimeException {
  private final String name;

  public RepositoryNotFoundException(String name) {
    super("no matching repository for " + name + " found");
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
