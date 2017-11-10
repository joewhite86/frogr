package de.whitefrog.examples.simpsons.rest;

import de.whitefrog.examples.simpsons.model.Character;
import de.whitefrog.examples.simpsons.repository.CharacterRepository;
import de.whitefrog.froggy.rest.service.CRUDService;

import javax.ws.rs.Path;

@Path("characters")
public class Characters extends CRUDService<CharacterRepository, Character> {
}
