package de.whitefrog.froggy.model.relationship

import de.whitefrog.froggy.model.Base
import de.whitefrog.froggy.model.Model

/**
 * Interface common to all relationship models.
 */
interface Relationship<From : Model, To : Model> : Base {
  var from: From
  var to: To
}
