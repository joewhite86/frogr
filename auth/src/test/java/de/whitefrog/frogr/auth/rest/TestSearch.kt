package de.whitefrog.frogr.auth.rest

import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.test.AuthTest
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.rest.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNull
import org.junit.Test
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType


class TestSearch: AuthTest() {
  private fun response(): GenericType<Response<User>> = object : GenericType<Response<User>>() {}
  
  @Test
  fun fetchOtherUsersPassword() {
    app.service().beginTx().use { tx ->
      val user = User()
      user.login = "fetchOtherUsersPassword"
      user.password = "fetchOtherUsersPassword"
      user.role = Role.User
      app.service().repository(User::class.java)
        .save(user)
      tx.success()
    }
    
    val target = webTarget
      .path("user")
      .queryParam("fields", "password")
      .queryParam("filter", "login:=rick")

    val response = target
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())

    assertThat(response.data).hasSize(1)
    assertNull(response.data[0].password) 
  }
}