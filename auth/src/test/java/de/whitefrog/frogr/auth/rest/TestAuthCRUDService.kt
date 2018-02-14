package de.whitefrog.frogr.auth.rest

import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.test.AuthTest
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.rest.response.FrogrResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response.Status
import kotlin.test.assertNotNull

class TestAuthCRUDService: AuthTest() {
  private fun response(): GenericType<FrogrResponse<User>> = object : GenericType<FrogrResponse<User>>() {}
  private val repository = app.service().repository(User::class.java)
  
  @Test
  fun create() {
    val user = User()
    user.login = "testCreate"
    user.password = "testCreate"
    user.role = Role.User
    
    val response = webTarget.path("user")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .post(Entity.json(Collections.singleton(user)))
    
    assertEquals(Status.CREATED.statusCode, response.status)
    val responseData = response.readEntity(response())
    assertNotNull(responseData)
    assertThat(responseData.data).isNotEmpty
    assertThat(responseData.data[0].login).isEqualTo(user.login)
  }

  @Test
  fun createError() {
    val user = User()
    user.login = "testCreate"

    val response = webTarget.path("user")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .post(Entity.json(Collections.singleton(user)))

    assertEquals(Status.BAD_REQUEST.statusCode, response.status)
  }
  
  @Test
  fun readByUuid() {
    val user = User()
    user.login = "readByUuid"
    user.password = "readByUuid"
    user.role = Role.User
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val response = webTarget.path("user/${user.uuid}")
      .queryParam("fields", "all")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())
    
    assertThat(response.data).hasSize(1)
    assertEquals(user.uuid, response.data[0].uuid)
    assertEquals(user.login, response.data[0].login)
  }

  @Test
  fun read() {
    val user = User()
    user.login = "read"
    user.password = "read"
    user.role = Role.User
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val response = webTarget.path("user")
      .queryParam("fields", "all")
      .queryParam("filter", "login:=${user.login}")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(user.uuid, response.data[0].uuid)
    assertEquals(user.login, response.data[0].login)
  }

  @Test
  fun readWithPost() {
    val user = User()
    user.login = "readWithPost"
    user.password = "readWithPost"
    user.role = Role.User
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val params = SearchParameter()
      .fields("all")
      .filter("login", user.login)
    val response = webTarget.path("user/search")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .post(Entity.json(params))
    assertEquals(Status.OK.statusCode, response.status)
    
    val responseData = response.readEntity(response())
    assertThat(responseData.data).hasSize(1)
    assertEquals(user.uuid, responseData.data[0].uuid)
    assertEquals(user.login, responseData.data[0].login)
  }
  
  @Test
  fun update() {
    val user = User()
    user.login = "update"
    user.password = "update"
    user.role = Role.User
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }
    
    user.field = "test"

    val response = webTarget.path("user")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .put(Entity.json(Collections.singleton(user)))
    
    assertEquals(Status.OK.statusCode, response.status)
    val responseData = response.readEntity(response())

    assertThat(responseData.data).isNotEmpty
    assertEquals(user.uuid, responseData.data[0].uuid)
    assertEquals(user.field, responseData.data[0].field)
  }
  
  @Test
  fun delete() {
    val user = User()
    user.login = "delete"
    user.password = "delete"
    user.role = Role.User
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val response = webTarget.path("user/${user.uuid}")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .delete()
    assertEquals(Status.NO_CONTENT.statusCode, response.status)
  }

  @Test
  fun otherUsersPasswordNotVisible() {
    app.service().beginTx().use { tx ->
      val user = User()
      user.login = "fetchOtherUsersPassword"
      user.password = "fetchOtherUsersPassword"
      user.role = Role.User
      repository.save(user)
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