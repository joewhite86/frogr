package de.whitefrog.froggy.example.rest;

import de.whitefrog.froggy.example.model.Person;
import de.whitefrog.froggy.example.repository.PersonRepository;
import de.whitefrog.froggy.rest.service.CRUDService;

import javax.ws.rs.Path;

@Path("persons")
public class Persons extends CRUDService<PersonRepository, Person> {
}
