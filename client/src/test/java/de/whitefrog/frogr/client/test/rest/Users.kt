package de.whitefrog.frogr.client.test.rest

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.rest.AuthCRUDService
import de.whitefrog.frogr.client.test.model.User
import de.whitefrog.frogr.client.test.repository.UserRepository
import de.whitefrog.frogr.rest.Views
import io.dropwizard.auth.Auth
import javax.annotation.security.RolesAllowed
import javax.ws.rs.*
import javax.ws.rs.core.Response

@Path("user")
@Produces("application/json")
class Users: AuthCRUDService<UserRepository, User, User>() {
  @POST
  @RolesAllowed(Role.User)
  @JsonView(Views.Secure::class)
  override fun create(@Auth user: User, models: List<User>): Response {
    models.forEach { it.role = Role.User }
    return super.create(user, models)
  }

  @GET
  @Path("login")
  @JsonView(Views.Secure::class)
  fun login(@QueryParam("login") login: String, @QueryParam("password") password: String): User {
    service().beginTx().use { tx ->
      val user = repository().login(login, password)
      tx.success()
      return user
    }
  }
  
  @POST
  @Path("logout")
  fun logout(@Auth user: User) {
    service().beginTx().use { tx ->
      repository().logout(user)
      tx.success()
    }
  }
}