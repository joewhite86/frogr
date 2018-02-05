package de.whitefrog.frogr.patch

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class TestPatcher {
  companion object {
    private var service: Service = TemporaryService()
    @BeforeClass
    @JvmStatic
    fun init() {
      service.connect()
    }
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