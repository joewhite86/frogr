package de.whitefrog.frogr.auth.rest

import de.whitefrog.frogr.auth.test.AuthTest
import de.whitefrog.frogr.auth.test.model.Person
import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.rest.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

class TestDefaultAuthCRUDService: AuthTest() {
  private fun response(): GenericType<Response<Person>> = object : GenericType<Response<Person>>() {}
  private val repository = app.service().repository(Person::class.java)

  @Test
  fun create() {
    val person = Person()
    person.field = "testCreate"

    val response = webTarget.path("person")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .post(Entity.json(Collections.singleton(person)))

    assertEquals(Status.CREATED.statusCode, response.status)
    val responseData = response.readEntity(response())
    assertNotNull(responseData)
    assertThat(responseData.data).isNotEmpty
    assertThat(responseData.data[0].field).isEqualTo(person.field)
  }

  @Test
  fun readByUuid() {
    val person = Person()
    person.field = "readByUuid"
    app.service().beginTx().use { tx ->
      repository.save(person)
      tx.success()
    }

    val response = webTarget.path("person/${person.uuid}")
      .queryParam("fields", "all")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(person.uuid, response.data[0].uuid)
    assertEquals(person.field, response.data[0].field)
  }

  @Test
  fun read() {
    val user = Person()
    user.field = "read"
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val response = webTarget.path("person")
      .queryParam("fields", "all")
      .queryParam("filter", "field:=${user.field}")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(user.uuid, response.data[0].uuid)
    assertEquals(user.field, response.data[0].field)
  }

  @Test
  fun readWithPost() {
    val person = Person()
    person.field = "readWithPost"
    app.service().beginTx().use { tx ->
      repository.save(person)
      tx.success()
    }

    val params = SearchParameter()
      .fields("all")
      .filter("field", person.field)
    val response = webTarget.path("person/search")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .post(Entity.json(params))
    assertEquals(Status.OK.statusCode, response.status)

    val responseData = response.readEntity(response())
    assertThat(responseData.data).hasSize(1)
    assertEquals(person.uuid, responseData.data[0].uuid)
    assertEquals(person.field, responseData.data[0].field)
  }

  @Test
  fun update() {
    val person = Person()
    person.field = "update"
    app.service().beginTx().use { tx ->
      repository.save(person)
      tx.success()
    }

    person.field = "test"

    val response = webTarget.path("person")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .put(Entity.json(Collections.singleton(person)))

    assertEquals(Status.OK.statusCode, response.status)
    val responseData = response.readEntity(response())

    assertThat(responseData.data).isNotEmpty
    assertEquals(person.uuid, responseData.data[0].uuid)
    assertEquals(person.field, responseData.data[0].field)
  }

  @Test
  fun delete() {
    val person = Person()
    person.field = "delete"
    app.service().beginTx().use { tx ->
      repository.save(person)
      tx.success()
    }

    val response = webTarget.path("person/${person.uuid}")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .delete()
    assertEquals(Status.NO_CONTENT.statusCode, response.status)
  }
}