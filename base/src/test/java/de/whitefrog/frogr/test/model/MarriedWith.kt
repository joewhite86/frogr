package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.relationship.FRelationship

class MarriedWith(from: Person, to: Person): FRelationship<Person, Person>(from, to)