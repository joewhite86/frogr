package de.whitefrog.neobase.test

import de.whitefrog.neobase.model.relationship.BaseRelationship

class Likes(from:Person, to:Person) : BaseRelationship<Person, Person>(from, to) {
    var field: String? = null
}
