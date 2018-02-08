package de.whitefrog.frogr.model

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class TestModel {
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
  fun newInstancesInHashSet() {
    service.beginTx().use {
      var set: MutableSet<Person> = HashSet()
      set.add(persons.createModel())
      set.add(persons.createModel())
      assertThat(set).hasSize(2)
      persons.save(*set.toTypedArray())
      set = HashSet(set)
      val first = persons.findByUuid(set.iterator().next().uuid)
      set.add(first)
      assertThat(set).hasSize(2)
    }
  }

  @Test
  fun bidirectionalRelationships() {
    service.beginTx().use {
      val man = persons.createModel()
      val woman = persons.createModel()
      persons.save(man, woman)
      man.marriedWith = woman
      persons.save(man)
      persons.fetch(woman, "marriedWith")
      assertThat(woman.marriedWith).isEqualTo(man)
      persons.fetch(man, true, FieldList.parseFields("marriedWith"))
      assertThat(man.marriedWith).isEqualTo(woman)
      // this should not throw an exception and create no additional relationship
      persons.save(woman)
      persons.fetch(woman, "marriedWith")
    }
  }
}
