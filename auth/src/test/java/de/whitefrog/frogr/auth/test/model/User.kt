package de.whitefrog.frogr.auth.test.model

import de.whitefrog.frogr.model.BaseUser
import org.hibernate.validator.constraints.Length

class User : BaseUser() {
  @Length(max = 5)
  var field: String? = null
}