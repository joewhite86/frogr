package de.whitefrog.froggy.rest;

public class Views {
  public static class Public {}
  public static class Secure extends Public {}
  public static class Hidden extends Secure {}
}
