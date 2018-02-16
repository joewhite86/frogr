package de.whitefrog.frogr.test.model

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.*
import de.whitefrog.frogr.rest.Views
import org.neo4j.graphdb.Direction
import java.util.*

class Person(@Indexed var field: String? = null, @Indexed var number: Long? = null) : Entity(), PersonInterface {
  enum class Age { Old, Mature, Child}
  @Unique
  var uniqueField: String? = null
  @Indexed(type = IndexType.LowerCase)
  var lowerCaseIndex: String? = null
  var age: Age? = null
  var dateField: Date? = null
  @Fetch
  var autoFetch: String? = null
  @JsonView(Views.Secure::class)
  var secureField: String? = null
  @NullRemove
  var nullRemoveField: String? = null
  @Lazy @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: ArrayList<Person> = ArrayList()
  @Lazy @RelatedTo(direction = Direction.INCOMING, type = "Likes")
  var likedBy: ArrayList<Person> = ArrayList()
  @Lazy @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likesRelationships: ArrayList<Likes> = ArrayList()
  @Lazy @RelatedTo(direction = Direction.INCOMING, type = "Likes")
  var likedByRelationships: ArrayList<Likes> = ArrayList()
  @RelationshipCount(direction = Direction.OUTGOING, type = "Likes")
  var likesCount: Long? = null
  @RelatedTo(direction = Direction.BOTH, type = "MarriedWith")
  var marriedWith: Person? = null
  @RelatedTo(direction = Direction.BOTH, type = "MarriedWith")
  var marriedWithRelationship: MarriedWith? = null
  @RelatedTo(direction = Direction.OUTGOING, type = "Wears")
  var wears: ArrayList<Clothing> = ArrayList()
  @RelatedTo(direction = Direction.OUTGOING, type = "HasInventory")
  var inventory: ArrayList<InventoryItem> = ArrayList()

  override fun equals(other: Any?): Boolean {
    if(other !is Person) return false
    val eq = super.equals(other)
    if(!eq && id < 0 && other.id < 0 && field != null) {
      return field == other.field
    }
    return eq
  }

  override fun toString(): String {
    val sField = if(field != null) " ($field)" else ""
    return "Person$sField"
  }
}
