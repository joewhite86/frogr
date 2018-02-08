package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class TestRelationshipRepository {
  companion object {
    private lateinit var service: Service
    private lateinit var persons: PersonRepository
    private lateinit var likesRepository: RelationshipRepository<Likes>

    @BeforeClass @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
      persons = service.repository(Person::class.java)
      likesRepository = service.repository(Likes::class.java)
    }

    @AfterClass @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test
  fun createRelationship() {
    service.beginTx().use {
      val model1 = persons.createModel()
      model1.field = "test1"
      val model2 = persons.createModel()
      model2.field = "test2"
      persons.save(model1, model2)

      var likes = Likes(model1, model2)
      likes.field = "test"
      likesRepository.save(likes)
      likes = likesRepository.find(likes.id, "from.field", "to.field", "field")
      Assertions.assertThat(likes).isNotNull()
      Assertions.assertThat(likes.field).isEqualTo("test")
      Assertions.assertThat(likes.from).isEqualTo(model1)
      Assertions.assertThat(likes.from.field).isEqualTo("test1")
      Assertions.assertThat(likes.to).isEqualTo(model2)
      Assertions.assertThat(likes.to.field).isEqualTo("test2")
    }
  }

  @Test
  fun createRelationship2() {
    service.beginTx().use {
      var model1 = persons.createModel()
      val model2 = persons.createModel()
      persons.save(model1, model2)

      model1.likes = ArrayList()
      model1.likes.add(model2)
      persons.save(model1)
      model1 = persons.find(model1.id, "likes")
      Assertions.assertThat(model1).isNotNull()
      Assertions.assertThat(model1.likes).hasSize(1)
      Assertions.assertThat(model1.likes[0]).isEqualTo(model2)
    }
  }
}