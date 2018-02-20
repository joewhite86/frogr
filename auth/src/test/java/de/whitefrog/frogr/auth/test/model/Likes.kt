package de.whitefrog.frogr.auth.test.model

import de.whitefrog.frogr.model.relationship.FRelationship

class Likes @JvmOverloads constructor(from: Person, to: Person, var field: String? = null) : 
  FRelationship<Person, Person>(from, to)
