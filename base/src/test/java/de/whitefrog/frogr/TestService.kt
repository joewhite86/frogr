package de.whitefrog.frogr

import de.whitefrog.frogr.test.TemporaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class TestService {
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
  fun isConnected() {
    assertThat(service.isConnected).isTrue()
  }
  @Test
  fun runningState() {
    assertThat(service.state).isEqualTo(Service.State.Running)
  }
  
  @Test
  fun indexesCreated() {
    service.beginTx().use {
      val schema = service.graph().schema()
      assertTrue(schema.indexes.any { it!!.propertyKeys.first() == "number" })
      assertTrue(schema.indexes.asSequence().any { it!!.propertyKeys.first() == "lowerCaseIndex_lower" })
    }
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
