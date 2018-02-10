package de.whitefrog.frogr.auth.test.model

import de.whitefrog.frogr.model.relationship.BaseRelationship

class MarriedWith(from: Person, to: Person): BaseRelationship<Person, Person>(from, to)