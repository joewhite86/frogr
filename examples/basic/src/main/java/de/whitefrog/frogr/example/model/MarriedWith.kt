package de.whitefrog.frogr.example.model

import de.whitefrog.frogr.model.relationship.BaseRelationship

class MarriedWith(from: Person, to: Person): BaseRelationship<Person, Person>(from, to) {
  var years: Long? = null
}