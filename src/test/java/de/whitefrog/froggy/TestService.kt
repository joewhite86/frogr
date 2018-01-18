package de.whitefrog.froggy

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

class TestService {
  @Test
  fun isConnected() {
    assertThat(TestSuite.service().isConnected).isTrue
  }
  @Test
  fun runningState() {
    assertThat(TestSuite.service().state).isEqualTo(Service.State.Running)
  }
  @Test
  fun setVersion() {
    TestSuite.service().version = "1.0.1"
    assertThat(TestSuite.service().version).isEqualTo("1.0.1")
  }
  @Test
  fun noManifestVersion() {
    System.clearProperty("version")
    assertThat(Service.getManifestVersion()).isEqualTo("undefined")
  }
  @Test
  fun snapshotManifestVersion() {
    System.setProperty("version", "1.0.1-SNAPSHOT")
    assertThat(Service.getManifestVersion()).isEqualTo("1.0.1")
  }
  @Test
  fun restartService() {
    TestSuite.service().shutdown()
    TestSuite.service().connect()
  }
}
