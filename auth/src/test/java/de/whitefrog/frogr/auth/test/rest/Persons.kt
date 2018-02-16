package de.whitefrog.frogr.auth.test.rest

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.rest.DefaultAuthCRUDService
import de.whitefrog.frogr.auth.test.model.Person
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.rest.Views
import de.whitefrog.frogr.rest.request.SearchParam
import de.whitefrog.frogr.rest.response.FrogrResponse
import javax.annotation.security.RolesAllowed
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("person")
class Persons: DefaultAuthCRUDService<Person, User>() {
  @GET
  @Path("insecure")
  @RolesAllowed(Role.Public)
  @JsonView(Views.Secure::class)
  fun searchInsecure(@SearchParam params: SearchParameter): FrogrResponse<Person>? {
    return super.search(null, params)
  }
}