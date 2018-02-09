package de.whitefrog.frogr

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestApplication {
  @Test
  fun register() {
    val app = de.whitefrog.frogr.test.TestApplication()
    app.register("test")
    assertThat(app.registry()).contains("test")
    app.register("test2;test3")
    assertThat(app.registry()).contains("test2")
    assertThat(app.registry()).contains("test3")
  }
}
