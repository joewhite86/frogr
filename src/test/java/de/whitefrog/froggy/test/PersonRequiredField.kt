package de.whitefrog.froggy.test

import de.whitefrog.froggy.model.Entity
import de.whitefrog.froggy.model.annotation.Required

class PersonRequiredField : Entity() {
  @Required
  var requiredField: String? = null
}
