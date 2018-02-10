package de.whitefrog.frogr.auth.rest

import de.whitefrog.frogr.auth.test.AuthTest
import de.whitefrog.frogr.auth.test.model.Person
import de.whitefrog.frogr.rest.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import kotlin.test.assertEquals

class TestDefaultAuthCRUDService: AuthTest() {
  private fun response(): GenericType<Response<Person>> = object : GenericType<Response<Person>>() {}

  @Test
  fun search() {
    app.service().beginTx().use { tx ->
      app.service().repository(Person::class.java)
        .save(Person("search"))
      tx.success()
    }

    val target = webTarget
      .path("person")
      .queryParam("fields", "field")
      .queryParam("filter", "field:=search")

    val response = target
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $token")
      .get(response())

    assertThat(response.data).hasSize(1)
    assertEquals(response.data[0].field, "search")
  }
}