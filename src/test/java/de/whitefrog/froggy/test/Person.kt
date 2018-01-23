package de.whitefrog.froggy.test

import de.whitefrog.froggy.model.Entity
import de.whitefrog.froggy.model.annotation.Lazy
import de.whitefrog.froggy.model.annotation.RelatedTo
import de.whitefrog.froggy.model.annotation.Unique
import de.whitefrog.froggy.model.annotation.Uuid
import org.neo4j.graphdb.Direction

class Person : Entity() {
  @Uuid
  @Unique
  var uniqueField: String? = null
  var field: String? = null
  var number: Long? = null
  @Lazy
  @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: List<Person>? = null
}
