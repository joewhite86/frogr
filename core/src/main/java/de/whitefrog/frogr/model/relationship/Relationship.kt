package de.whitefrog.frogr.model.relationship

import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.Model

/**
 * Interface common to all relationship models.
 */
interface Relationship<From : Model, To : Model> : Base {
  var from: From
  var to: To
  
  companion object {
    const val From = "from"
    const val To = "to"
  }
}
