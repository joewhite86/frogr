package de.whitefrog.frogr.health

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class TestGraphHealthCheck {
  companion object {
    private lateinit var service: Service

    @BeforeClass @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
    }

    @AfterClass @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test
  fun check() {
    val checker = GraphHealthCheck(service)
    val result = checker.check()
    assertThat(result.isHealthy).isTrue()
  }
}