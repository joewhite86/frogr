package de.whitefrog.frogr.health

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class TestGraphHealthCheck {
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
  fun check() {
    val checker = GraphHealthCheck(service)
    val result = checker.check()
    assertThat(result.isHealthy).isTrue()
  }
}