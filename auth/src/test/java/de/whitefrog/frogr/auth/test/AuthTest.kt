package de.whitefrog.frogr.auth.test

import de.whitefrog.frogr.auth.model.Role
import de.whitefrog.frogr.auth.test.model.User
import de.whitefrog.frogr.rest.response.FrogrResponse
import io.dropwizard.Configuration
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType

open class AuthTest {
  companion object {
    @ClassRule @JvmField
    val Rule = DropwizardAppRule<Configuration>(TestApplication::class.java, 
      ResourceHelpers.resourceFilePath("config/test.yml"))
    lateinit var app: TestApplication
    lateinit var client: Client
    lateinit var webTarget: WebTarget
    var token: String? = null

    @BeforeClass @JvmStatic
    fun before() {
      app = Rule.getApplication()
      client = ClientBuilder.newClient()
      webTarget = client.target("http://localhost:8282")

      app.service().beginTx().use { tx ->
        val users = app.service().repository(User::class.java)
        val user = User()
        user.login = "rick"
        user.password = "morty"
        user.role = Role.User
        users.save(user)
        tx.success()
      }

      val target = webTarget.path("user/login")
      val response = target
        .queryParam("login", "rick")
        .queryParam("password", "morty")
        .request(MediaType.APPLICATION_JSON)
        .get(object : GenericType<FrogrResponse<User>>() {})
      token = response.data[0].accessToken
    }

    @AfterClass @JvmStatic
    fun after() {
      app.shutdown()
    }
  }
}