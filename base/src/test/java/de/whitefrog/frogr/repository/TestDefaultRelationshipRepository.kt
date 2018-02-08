package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class TestDefaultRelationshipRepository {
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
  fun defaultRepository() {
    val repository = service.repository(Likes::class.java)
    assertThat(repository).isInstanceOfAny(DefaultRelationshipRepository::class.java)
    assertThat(service.repositoryFactory().cache()).contains(repository)
  }
}