package de.whitefrog.frogr.client.test.model

import de.whitefrog.frogr.model.BaseUser
import de.whitefrog.frogr.model.annotation.Unique
import org.hibernate.validator.constraints.Length

class User : BaseUser() {
  @Length(max = 5)
  var field: String? = null
  @Unique
  var uniqueField: String? = null
}