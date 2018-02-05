package de.whitefrog.frogr

import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class TestService {
  companion object {
    private var service: Service = TemporaryService()
    @BeforeClass @JvmStatic
    fun init() {
      service.connect()
    }
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
    assertThat(service.manifestVersion).isEqualTo("undefined")
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
  
  @Test
  fun applyPatch() {
    service.shutdown()
    System.setProperty("version", "10.0.0")
    service.connect()
    assertThat(service.repository(Person::class.java).search().count()).isEqualTo(1)
    System.clearProperty("version")
  }
}
