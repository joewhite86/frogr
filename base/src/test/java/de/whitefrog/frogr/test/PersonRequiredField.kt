package de.whitefrog.frogr.test

import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.Required

class PersonRequiredField : Entity() {
  @Required
  var requiredField: String? = null
}
