package de.whitefrog.froggy.rest;

/**
 * Visibility classes for REST services.
 * Public is visibly to everyone.
 * Secure is only visible to the owning user itself.
 * Hidden is not available with REST.
 */
public class Views {
  public static class Public {}
  public static class Secure extends Public {}
  public static class Hidden extends Secure {}
}
