package de.whitefrog.examples.simpsons.rest;

import de.whitefrog.examples.simpsons.model.Person;
import de.whitefrog.examples.simpsons.repository.PersonRepository;
import de.whitefrog.neobase.rest.service.RestService;

public class Persons extends RestService<PersonRepository, Person> {
}
