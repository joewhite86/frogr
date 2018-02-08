package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.PersistException
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.model.PersonInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

class TestDefaultModelRepository {
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
    val repository = service.repository(Clothing::class.java)
    assertThat(repository).isInstanceOf(DefaultModelRepository::class.java)
    assertThat(service.repositoryFactory().cache()).contains(repository)
  }
  
  @Test
  fun interfaceRepository() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)
      val person = Person("test")
      persons.save(person)
      val repository = service.repository(PersonInterface::class.java)
      assertThat(repository.modelClass).isEqualTo(PersonInterface::class.java)
      assertThat(repository.find(person.id)).isNotNull()
    }
  }

  @Test(expected = PersistException::class)
  fun saveInInterfaceRepository() {
    service.beginTx().use {
      val person = Person("test")
      val repository = service.repository(PersonInterface::class.java)
      repository.save(person)
    }
  }
}