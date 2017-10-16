package de.whitefrog.froggy.model.relationship

import de.whitefrog.froggy.model.Base

/**
 * Interface common to all relationship models.
 */
interface Relationship<out From, out To> : Base {
  val from: From
  val to: To
}
