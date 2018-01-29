package de.whitefrog.frogr.test

import de.whitefrog.frogr.model.relationship.BaseRelationship

class Likes(from:Person, to:Person) : BaseRelationship<Person, Person>(from, to) {
    var field: String? = null
}
