package de.whitefrog.frogr.test.rest

import de.whitefrog.frogr.rest.service.CRUDService
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository

class Persons: CRUDService<PersonRepository, Person>()