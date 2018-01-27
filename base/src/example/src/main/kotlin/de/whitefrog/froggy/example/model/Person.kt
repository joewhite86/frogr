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
  
  // Unique and required property
  @Unique
  @Indexed
  @Required
  var name: String? = null

  // Relationship to another single model
  @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
  var marriedWith: Person? = null
  // Relationship to a collection of models
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  var parents: List<Person> = ArrayList()
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.INCOMING)
  var children: List<Person> = ArrayList()
}
