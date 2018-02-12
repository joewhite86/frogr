package de.whitefrog.frogr.auth.example.model

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.auth.example.RelationshipTypes
import de.whitefrog.frogr.auth.model.BaseUser
import de.whitefrog.frogr.model.annotation.RelatedTo
import de.whitefrog.frogr.rest.Views
import java.util.*

class User : BaseUser() {
  @JsonView(Views.Secure::class)
  @RelatedTo(type = RelationshipTypes.FriendWith)
  var friends: ArrayList<User> = ArrayList()
}