package de.whitefrog.frogr.benchmark

import de.whitefrog.frogr.rest.response.FrogrResponse
import de.whitefrog.frogr.test.BaseBenchmark
import de.whitefrog.frogr.test.Benchmark
import de.whitefrog.frogr.test.TestApplication
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Person
import io.dropwizard.Configuration
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType

@Ignore
class BenchmarkRestSearch: BaseBenchmark() {
  companion object {
    @ClassRule
    @JvmField
    val Rule = DropwizardAppRule<Configuration>(TestApplication::class.java, ResourceHelpers.resourceFilePath("config/test.yml"))
    private lateinit var app: TestApplication
    private lateinit var client: Client
    private lateinit var webTarget: WebTarget
    private const val initPersons = 5000
    private const val searchCount = 250000

    private val personList = ArrayList<Person>(initPersons)

    @BeforeClass @JvmStatic
    fun startup() {
      app = Rule.getApplication()
      client = ClientBuilder.newClient()
      webTarget = client.target("http://localhost:8282")

      println("initializing data")
      val persons = app.service().repository(Person::class.java)
      val clothes = app.service().repository(Clothing::class.java)
      var tx = app.service().beginTx()
      personList.clear()
      for(i in (0..initPersons)) {
        val person = Person("person$i")
        person.uniqueField = "person_unique$i"
        person.secureField = "person_secure$i"
        person.autoFetch = "person_autofetch$i"
        person.number = 100L + i

        if(i > 2) {
          var rand = Math.floor(Math.random() * personList.size).toLong()
          person.likes.add(personList[rand.toInt()])
          rand = Math.floor(Math.random() * personList.size).toLong()
          person.likes.add(personList[rand.toInt()])
        }

        person.wears.add(Clothing("clothing$i"))
        person.wears.add(Clothing("clothing${initPersons+i+1}"))

        clothes.save(*person.wears.toTypedArray())
        persons.save(person)
        personList.add(person)
        if(i > 0 && i % 50000 == 0) {
          tx.success()
          tx.close()
          println("$i entries created")
          tx = app.service().beginTx()
        }
      }
      tx.success()
      tx.close()
      println("benchmark prepared")
    }

    private fun response(): GenericType<FrogrResponse<Person>> = object : GenericType<FrogrResponse<Person>>() {}

    @AfterClass @JvmStatic
    fun shutdown() {
      app.shutdown()
    }
  }

  @Test
  @Benchmark(expectation = 200, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun read() {
    for(i in (0..searchCount)) {
      val rand = Math.floor(Math.random() * initPersons).toInt()
      webTarget.path("person")
        .queryParam("fields", "field,secureField")
        .queryParam("filter", "field:=${personList[rand].field}")
        .request(MediaType.APPLICATION_JSON)
        .get(response())
    }
  }
}