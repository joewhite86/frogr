package de.whitefrog.frogr.auth.test.rest

import de.whitefrog.frogr.auth.rest.DefaultAuthCRUDService
import de.whitefrog.frogr.auth.test.model.Person
import de.whitefrog.frogr.auth.test.model.User
import javax.ws.rs.Path

@Path("person")
class Persons: DefaultAuthCRUDService<Person, User>()