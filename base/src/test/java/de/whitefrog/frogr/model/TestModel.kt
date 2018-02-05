package de.whitefrog.frogr.model

import de.whitefrog.frogr.TestSuite
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class TestModel {
  companion object {
    private var service = TestSuite.service()
    private lateinit var persons: PersonRepository
    private lateinit var likesRepository: RelationshipRepository<Likes>

    @JvmStatic
    @BeforeClass
    fun init() {
      persons = service.repository(Person::class.java)
      likesRepository = service.repository(Likes::class.java)
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
