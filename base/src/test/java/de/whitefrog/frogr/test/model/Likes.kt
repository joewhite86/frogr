package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.relationship.BaseRelationship

class Likes(from: Person, to: Person, var field: String? = null) : BaseRelationship<Person, Person>(from, to)
