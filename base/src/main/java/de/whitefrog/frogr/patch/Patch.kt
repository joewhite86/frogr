package de.whitefrog.frogr.patch

import com.github.zafarkhaja.semver.Version
import de.whitefrog.frogr.Service

/**
 * Database patch for bulk operations and field changes.
 */
abstract class Patch(val service: Service) : Comparable<Patch> {
  val version: Version = Version.valueOf(javaClass.getAnnotation(de.whitefrog.frogr.patch.Version::class.java).value)
  val priority: Int = javaClass.getAnnotation(de.whitefrog.frogr.patch.Version::class.java).priority

  abstract fun update()

  override fun compareTo(o: Patch): Int {
    return if (version.greaterThan(o.version) || priority < o.priority)
      1
    else if (version == o.version && priority == o.priority)
      0
    else
      -1
  }
}
