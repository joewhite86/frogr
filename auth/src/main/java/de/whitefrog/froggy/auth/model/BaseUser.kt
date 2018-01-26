package de.whitefrog.froggy.auth.model

import de.whitefrog.froggy.model.Entity
import de.whitefrog.froggy.model.annotation.NotPersistant

import javax.security.auth.Subject
import java.security.Principal

open class BaseUser : Entity(), Principal {
  @NotPersistant
  private val name: String? = null
  var role: String? = null
  var accessToken: String? = null

  override fun getName(): String? {
    return name
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
