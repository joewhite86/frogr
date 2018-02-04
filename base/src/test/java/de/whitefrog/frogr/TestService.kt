package de.whitefrog.frogr

import de.whitefrog.frogr.test.TemporaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestService {
  private var service: Service = TemporaryService()
  init { service.connect() }
  
  @Test
  fun isConnected() {
    assertThat(service.isConnected).isTrue()
  }
  @Test
  fun runningState() {
    assertThat(service.state).isEqualTo(Service.State.Running)
  }
  @Test
  fun setVersion() {
    service.version = "1.0.1"
    assertThat(service.version).isEqualTo("1.0.1")
  }
  @Test
  fun noManifestVersion() {
    System.clearProperty("version")
    assertThat(service.manifestVersion).isEqualTo("undefined")
  }
  @Test
  fun snapshotManifestVersion() {
    System.setProperty("version", "1.0.1-SNAPSHOT")
    assertThat(service.manifestVersion).isEqualTo("1.0.1")
  }
  
  @Test
  fun restartService() {
    service.shutdown()
    service.connect()
  }
}
