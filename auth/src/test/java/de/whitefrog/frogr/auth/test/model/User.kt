package de.whitefrog.frogr.auth.test.model

import de.whitefrog.frogr.auth.model.BaseUser
import org.hibernate.validator.constraints.Length

class User : BaseUser() {
  @Length(max = 5)
  var field: String? = null
}