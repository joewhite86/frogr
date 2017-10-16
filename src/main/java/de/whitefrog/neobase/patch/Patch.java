package de.whitefrog.neobase.patch;

import com.github.zafarkhaja.semver.Version;
import de.whitefrog.neobase.Service;

/**
 * Database patch for bulk operations and field changes.
 */
public abstract class Patch implements Comparable<Patch> {
  private Service service;
  private final Version version;
  private int priority = 0;

  public Patch(Service service) {
    this.service = service;
    this.version = Version.valueOf(getClass().getAnnotation(de.whitefrog.neobase.patch.Version.class).value());
  }

  public Patch(String version, Service service) {
    this.service = service;
    this.version = Version.valueOf(version);
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

  public abstract void update();

  public Version getVersion() {
    return version;
  }

  @Override
  public int compareTo(Patch o) {
    if(getVersion().greaterThan(o.getVersion()) || getPriority() < o.getPriority()) return 1;
    else if(getVersion().equals(o.getVersion()) && getPriority() == o.getPriority()) return 0;
    else return -1;
  }
}
