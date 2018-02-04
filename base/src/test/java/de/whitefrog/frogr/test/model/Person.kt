package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.*
import org.neo4j.graphdb.Direction
import java.util.*

class Person : Entity() {
  enum class Age { Old, Mature, Child}
  @Uuid
  @Unique
  var uniqueField: String? = null
  var field: String? = null
  var number: Long? = null
  var age: Age? = null
  var dateField: Date? = null
  @NullRemove
  var nullRemoveField: String? = null
  @Lazy
  @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: ArrayList<Person>? = null
  @RelatedTo(direction = Direction.BOTH, type = "MarriedWith")
  var marriedWith: Person? = null
}
