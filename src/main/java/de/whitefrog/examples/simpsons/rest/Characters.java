package de.whitefrog.examples.simpsons.rest;

import com.fasterxml.jackson.annotation.JsonView;
import de.whitefrog.examples.simpsons.model.Character;
import de.whitefrog.examples.simpsons.repository.CharacterRepository;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.rest.CRUDService;
import de.whitefrog.froggy.model.rest.SearchParameter;
import de.whitefrog.froggy.rest.Views;
import de.whitefrog.froggy.rest.request.SearchParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("characters")
public class Characters extends CRUDService<CharacterRepository, Character> {
}
