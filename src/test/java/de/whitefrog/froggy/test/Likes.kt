package de.whitefrog.froggy.test

import de.whitefrog.froggy.model.relationship.BaseRelationship

class Likes(from:Person, to:Person) : BaseRelationship<Person, Person>(from, to) {
    var field: String? = null
}
