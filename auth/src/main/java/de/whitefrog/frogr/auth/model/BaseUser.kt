package de.whitefrog.frogr.auth.model

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.*
import de.whitefrog.frogr.rest.Views

import javax.security.auth.Subject
import java.security.Principal

open class BaseUser : Entity(), Principal {
  @Unique @Fetch @Required
  open var login: String? = null
  @JsonView(Views.Hidden::class)
  open var password: String? = null
  var role: String? = null
  @Indexed
  @JsonView(Views.Secure::class)
  var accessToken: String? = null

  override fun getName(): String? {
    return login
  }

  override fun implies(subject: Subject?): Boolean {
    return subject!!.principals.any { p -> p.name == name }
  }
  companion object {
    @JvmField val Login = "login"
    @JvmField val Password = "password"
    @JvmField val AccessToken = "accessToken"
    @JvmField val Roles = "role"
  }
}
