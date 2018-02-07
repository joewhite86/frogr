package de.whitefrog.frogr

import de.whitefrog.frogr.test.TemporaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class TestService {
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
    assertThat(service.manifestVersion).isEqualTo("0.0.0")
  }
  @Test
  fun snapshotManifestVersion() {
    System.setProperty("version", "1.0.1-SNAPSHOT")
    assertThat(service.manifestVersion).isEqualTo("1.0.1")
    System.clearProperty("version")
  }
  
  @Test
  fun restartService() {
    service.shutdown()
    service.connect()
  }
}
