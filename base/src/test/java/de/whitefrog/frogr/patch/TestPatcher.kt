package de.whitefrog.frogr.patch

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class TestPatcher {
  private lateinit var service: Service

  @Before
  fun before() {
    service = TemporaryService()
    service.connect()
  }
  @After
  fun after() {
    service.shutdown()
  }

  @Test
  fun applyPatch() {
    val repository = service.repository(Person::class.java)
    service.shutdown()
    System.setProperty("version", "10.0.0")
    service.connect()
    System.clearProperty("version")
    service.beginTx().use {
      assertThat(repository.search().count()).isEqualTo(3)
    }
  }
}