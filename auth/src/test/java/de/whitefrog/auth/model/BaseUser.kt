package de.whitefrog.auth.model

import de.whitefrog.froggy.auth.model.BaseUser

class BaseUser : BaseUser() {
  var login: String? = null
  var password: String? = null
}
