package de.whitefrog.frogr.example.model

import de.whitefrog.frogr.example.RelationshipTypes
import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.Indexed
import de.whitefrog.frogr.model.annotation.RelatedTo
import de.whitefrog.frogr.model.annotation.Required
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.model.annotation.Lazy
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
  @Lazy @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  var parents: List<Person> = ArrayList()
  @Lazy @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.INCOMING)
  var children: List<Person> = ArrayList()
  
  companion object {
    @JvmField val Name = "name"
    @JvmField val MarriedWith = "marriedWith"
    @JvmField val Children = "children"
  }
}
