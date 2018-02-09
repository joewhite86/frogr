package de.whitefrog.frogr.patch;

import com.github.zafarkhaja.semver.Version;
import de.whitefrog.frogr.Service;

/**
 * Database patch for bulk operations and field changes.
 */
public abstract class Patch implements Comparable<Patch> {
  private Service service;
  private final Version version;
  private final int priority;

  public Patch(Service service) {
    this.service = service;
    this.version = Version.valueOf(getClass().getAnnotation(de.whitefrog.frogr.patch.Version.class).value());
    this.priority = getClass().getAnnotation(de.whitefrog.frogr.patch.Version.class).proority();
  }

  public int getPriority() {
    return priority;
  }

  public Service getService() {
    return service;
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
