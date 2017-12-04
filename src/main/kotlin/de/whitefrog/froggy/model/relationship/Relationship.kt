package de.whitefrog.froggy.model.relationship

import de.whitefrog.froggy.model.Base
import de.whitefrog.froggy.model.Model

/**
 * Interface common to all relationship models.
 */
interface Relationship<out From : Model, out To : Model> : Base {
  val from: From
  val to: To
}
