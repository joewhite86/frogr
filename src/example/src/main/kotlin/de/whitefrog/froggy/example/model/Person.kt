package de.whitefrog.froggy.example.model

import de.whitefrog.froggy.example.RelationshipTypes
import de.whitefrog.froggy.model.Entity
import de.whitefrog.froggy.model.annotation.Indexed
import de.whitefrog.froggy.model.annotation.RelatedTo
import de.whitefrog.froggy.model.annotation.Required
import de.whitefrog.froggy.model.annotation.Unique
import org.neo4j.graphdb.Direction

class Person() : Entity() {
  constructor(name: String) : this() {
    this.name = name
  }
  @Unique
  @Indexed
  @Required
  var name: String? = null

  @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
  var marriedWith: Person? = null
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  var parents: List<Person>? = null
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.INCOMING)
  var children: List<Person>? = null
}
