package de.whitefrog.frogr.health

import de.whitefrog.frogr.TestSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestGraphHealthCheck {
  val service = TestSuite.service()
  
  @Test
  fun check() {
    val checker = GraphHealthCheck(service)
    val result = checker.check()
    assertThat(result.isHealthy).isTrue()
  }
}