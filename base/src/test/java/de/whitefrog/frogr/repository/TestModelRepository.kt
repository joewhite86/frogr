package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.model.PersonInterface
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.neo4j.graphdb.Label
import java.util.*

class TestModelRepository {
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
  fun deleteWithRelationship() {
    service.beginTx().use {
      val person = Person()
      val person2 = Person()
      persons.save(person, person2)
      person.marriedWith = person2
      persons.save(person)
      assertThat(persons.find(person.id, "marriedWith").marriedWith).isEqualTo(person2)
      persons.remove(person)
      assertThat(persons.find(person.id)).isNull()
    }
  }

  // There will not be an exception, else we run into problems
  // on not named relationships that already exist
  @Ignore
  @Test(expected = DuplicateEntryException::class)
  fun createDuplicateRelationship() {
    service.beginTx().use {
      val model1 = persons.createModel()
      val model2 = persons.createModel()
      persons.save(model1, model2)

      model1.likes = ArrayList()
      model1.likes.add(model2)
      persons.save(model1)

      model1.likes.clear()
      model1.likes.add(model2)
      persons.save(model1)

      val duplicate = Likes(model1, model2)
      duplicate.field = "test"
      likesRepository.save(duplicate)
    }
  }

//  @Test(expected = RepositoryInstantiationException::class)
//  fun invalidRepository() {
//    service.repository("Invalid")
//  }


  @Test
  fun modelWithInterface() {
    val person = Person()
    assertThat(person).isInstanceOf(PersonInterface::class.java)
    assertThat(persons.labels()).hasSize(1)
    assertThat(persons.labels()).contains(Label.label(PersonInterface::class.java.simpleName))
  }
}
