package de.whitefrog.frogr.rest.service

import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.rest.response.FrogrResponse
import de.whitefrog.frogr.test.TestApplication
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import io.dropwizard.Configuration
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.util.*
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response.Status

class TestCRUDService {
  companion object {
    @ClassRule
    @JvmField
    val Rule = DropwizardAppRule<Configuration>(TestApplication::class.java, ResourceHelpers.resourceFilePath("config/test.yml"))
    private lateinit var app: TestApplication
    private lateinit var client: Client
    private lateinit var webTarget: WebTarget
    private lateinit var repository: PersonRepository

    @BeforeClass
    @JvmStatic
    fun before() {
      app = Rule.getApplication()
      repository = app.service().repository(Person::class.java)
      client = ClientBuilder.newClient()
      webTarget = client.target("http://localhost:8282")
    }

    private fun response(): GenericType<FrogrResponse<Person>> = object : GenericType<FrogrResponse<Person>>() {}

    @AfterClass
    @JvmStatic
    fun after() {
      app.shutdown()
    }
  }

  @Test
  fun create() {
    val person = Person()
    person.field = "testCreate"

    val response = webTarget.path("person")
      .request(MediaType.APPLICATION_JSON)
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
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(person.uuid, response.data[0].uuid)
    assertEquals(person.field, response.data[0].field)
  }

  @Test
  fun read() {
    val user = Person()
    user.field = "read"
    user.autoFetch = "read"
    user.secureField = "secure"
    app.service().beginTx().use { tx ->
      repository.save(user)
      tx.success()
    }

    val response = webTarget.path("person")
      .queryParam("fields", "field,secureField")
      .queryParam("filter", "field:=${user.field}")
      .request(MediaType.APPLICATION_JSON)
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(-1L, response.data[0].id)
    assertNotNull(response.data[0].autoFetch)
    assertNull(response.data[0].secureField)
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
      .delete()
    assertEquals(Status.NO_CONTENT.statusCode, response.status)
  }
}