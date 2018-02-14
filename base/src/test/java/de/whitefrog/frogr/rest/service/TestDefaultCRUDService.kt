package de.whitefrog.frogr.rest.service

import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.repository.ModelRepository
import de.whitefrog.frogr.rest.response.FrogrResponse
import de.whitefrog.frogr.test.TestApplication
import de.whitefrog.frogr.test.model.Clothing
import io.dropwizard.Configuration
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

class TestDefaultCRUDService {
  companion object {
    @ClassRule
    @JvmField
    val Rule = DropwizardAppRule<Configuration>(TestApplication::class.java, ResourceHelpers.resourceFilePath("config/test.yml"))
    private lateinit var app: TestApplication
    private lateinit var client: Client
    private lateinit var webTarget: WebTarget
    private lateinit var repository: ModelRepository<Clothing>

    @BeforeClass
    @JvmStatic
    fun before() {
      app = Rule.getApplication()
      repository = app.service().repository(Clothing::class.java)
      client = ClientBuilder.newClient()
      webTarget = client.target("http://localhost:8282")
    }

    private fun response(): GenericType<FrogrResponse<Clothing>> = object : GenericType<FrogrResponse<Clothing>>() {}

    @AfterClass
    @JvmStatic
    fun after() {
      app.shutdown()
    }
  }

  @Test
  fun create() {
    val clothing = Clothing()
    clothing.name = "testCreate"

    val response = webTarget.path("clothing")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.json(Collections.singleton(clothing)))

    assertEquals(Status.CREATED.statusCode, response.status)
    val responseData = response.readEntity(response())
    assertNotNull(responseData)
    assertThat(responseData.data).isNotEmpty
    assertThat(responseData.data[0].name).isEqualTo(clothing.name)
  }

  @Test
  fun readByUuid() {
    val clothing = Clothing()
    clothing.name = "readByUuid"
    app.service().beginTx().use { tx ->
      repository.save(clothing)
      tx.success()
    }

    val response = webTarget.path("clothing/${clothing.uuid}")
      .queryParam("fields", "all")
      .request(MediaType.APPLICATION_JSON)
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(clothing.uuid, response.data[0].uuid)
    assertEquals(clothing.name, response.data[0].name)
  }

  @Test
  fun read() {
    val clothing = Clothing()
    clothing.name = "read"
    app.service().beginTx().use { tx ->
      repository.save(clothing)
      tx.success()
    }

    val response = webTarget.path("clothing")
      .queryParam("fields", "all")
      .queryParam("filter", "name:=${clothing.name}")
      .request(MediaType.APPLICATION_JSON)
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(clothing.uuid, response.data[0].uuid)
    assertEquals(clothing.name, response.data[0].name)
  }

  @Test
  fun readWithPost() {
    val clothing = Clothing()
    clothing.name = "readWithPost"
    app.service().beginTx().use { tx ->
      repository.save(clothing)
      tx.success()
    }

    val params = SearchParameter()
      .fields("all")
      .filter("name", clothing.name)
    val response = webTarget.path("clothing/search")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.json(params))
    assertEquals(Status.OK.statusCode, response.status)

    val responseData = response.readEntity(response())
    assertThat(responseData.data).hasSize(1)
    assertEquals(clothing.uuid, responseData.data[0].uuid)
    assertEquals(clothing.name, responseData.data[0].name)
  }

  @Test
  fun update() {
    val clothing = Clothing()
    clothing.name = "update"
    app.service().beginTx().use { tx ->
      repository.save(clothing)
      tx.success()
    }

    clothing.name = "test"

    val response = webTarget.path("clothing")
      .request(MediaType.APPLICATION_JSON)
      .put(Entity.json(Collections.singleton(clothing)))

    assertEquals(Status.OK.statusCode, response.status)
    val responseData = response.readEntity(response())

    assertThat(responseData.data).isNotEmpty
    assertEquals(clothing.uuid, responseData.data[0].uuid)
    assertEquals(clothing.name, responseData.data[0].name)
  }

  @Test
  fun delete() {
    val clothing = Clothing()
    clothing.name = "delete"
    app.service().beginTx().use { tx ->
      repository.save(clothing)
      tx.success()
    }

    val response = webTarget.path("clothing/${clothing.uuid}")
      .request(MediaType.APPLICATION_JSON)
      .delete()
    assertEquals(Status.NO_CONTENT.statusCode, response.status)
  }
}