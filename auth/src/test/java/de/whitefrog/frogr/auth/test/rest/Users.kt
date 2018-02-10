package de.whitefrog.frogr.auth.test.rest

import de.whitefrog.frogr.auth.rest.AuthCRUDService
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.auth.test.repository.UserRepository
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@Path("user")
class Users: AuthCRUDService<UserRepository, User, User>() {
  @GET
  @Path("login")
  fun login(@QueryParam("login") login: String, @QueryParam("password") password: String): User {
    service().beginTx().use { tx ->
      val user = repository().login(login, password)
      tx.success()
      return user
    }
  }
}