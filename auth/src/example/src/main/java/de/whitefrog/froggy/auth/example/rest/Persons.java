package de.whitefrog.froggy.auth.example.rest;

import de.whitefrog.froggy.auth.example.model.Person;
import de.whitefrog.froggy.auth.example.model.User;
import de.whitefrog.froggy.auth.example.repository.PersonRepository;
import de.whitefrog.froggy.auth.rest.AuthCRUDService;

import javax.ws.rs.Path;

@Path("persons")
public class Persons extends AuthCRUDService<PersonRepository, Person, User> {
}
