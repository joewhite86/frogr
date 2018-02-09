package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.relationship.BaseRelationship

class Likes @JvmOverloads constructor(from: Person, to: Person, var field: String? = null) : 
  BaseRelationship<Person, Person>(from, to)
