package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.model.annotation.Fetch
import de.whitefrog.frogr.model.annotation.Indexed
import de.whitefrog.frogr.model.annotation.Required
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.rest.Views
import java.security.Principal
import javax.security.auth.Subject

/**
 * Base user class required for Authentication.
 */
open class BaseUser : Entity(), Principal {
  /**
   * Unique and required field.
   */
  @Unique @Fetch @Required 
  open var login: String? = null

  /**
   * The password as string.
   */
  @JsonView(Views.Hidden::class)
  open var password: String? = null

  /**
   * [Role] in which the user is
   */
  @JsonView(Views.Hidden::class)
  open var role: String? = null

  /**
   * Used for oAuth user authentication.
   */
  @Indexed @JsonView(Views.Secure::class)
  var accessToken: String? = null

  override fun getName(): String? {
    return login
  }

  override fun implies(subject: Subject?): Boolean {
    return subject!!.principals.any { p -> (p as BaseUser).id == id }
  }

  override fun equals(other: Any?): Boolean {
    return super<Entity>.equals(other)
  }

  companion object {
    const val Login = "login"
    const val Password = "password"
    const val AccessToken = "accessToken"
    const val Roles = "role"
  }
}
