package de.whitefrog.froggy

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore

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
    assertThat(TestSuite.service().manifestVersion).isEqualTo("undefined")
  }
  @Test
  fun snapshotManifestVersion() {
    System.setProperty("version", "1.0.1-SNAPSHOT")
    assertThat(TestSuite.service().manifestVersion).isEqualTo("1.0.1")
  }
  
  @Test
  @Ignore
  fun restartService() {
    TestSuite.service().shutdown()
    TestSuite.service().connect()
  }
}
