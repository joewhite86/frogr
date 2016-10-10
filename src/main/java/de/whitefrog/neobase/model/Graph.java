package de.whitefrog.neobase.model;

public class Graph extends Entity {
  public static final String Version = "version";
  private String version;

  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }
}
