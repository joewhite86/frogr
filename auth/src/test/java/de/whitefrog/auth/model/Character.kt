package de.whitefrog.auth.model

import de.whitefrog.auth.RelationshipTypes
import de.whitefrog.froggy.model.Entity
import de.whitefrog.froggy.model.annotation.RelatedTo
import de.whitefrog.froggy.model.annotation.Unique
import org.neo4j.graphdb.Direction

class Character : Entity() {
  @Unique
  var name: String? = null
  @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
  var marriedWith: Character? = null
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  var parents: List<Character>? = null
}
