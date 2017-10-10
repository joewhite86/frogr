package de.whitefrog.neobase.model.relationship

import de.whitefrog.neobase.model.Base

/**
 * Interface common to all relationship models.
 */
interface Relationship<out From, out To> : Base {
  val from: From
  val to: To
}
