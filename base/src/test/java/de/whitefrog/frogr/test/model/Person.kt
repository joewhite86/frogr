package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.Lazy
import de.whitefrog.frogr.model.annotation.RelatedTo
import de.whitefrog.frogr.model.annotation.Unique
import de.whitefrog.frogr.model.annotation.Uuid
import org.neo4j.graphdb.Direction

class Person : Entity() {
  @Uuid
  @Unique
  var uniqueField: String? = null
  var field: String? = null
  var number: Long? = null
  @Lazy
  @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: ArrayList<Person>? = null
  @RelatedTo(direction = Direction.BOTH, type = "MarriedWith")
  var marriedWith: Person? = null
}
