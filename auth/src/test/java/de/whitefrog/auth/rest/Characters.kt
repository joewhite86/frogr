package de.whitefrog.auth.rest

import de.whitefrog.auth.model.Character
import de.whitefrog.auth.repository.CharacterRepository
import de.whitefrog.froggy.auth.model.BaseUser
import de.whitefrog.froggy.auth.rest.AuthCRUDService
import javax.ws.rs.Path

@Path("characters")
class Characters : AuthCRUDService<CharacterRepository, Character, BaseUser>()
