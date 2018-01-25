package de.whitefrog.froggy

import de.whitefrog.froggy.model.rest.FieldList
import de.whitefrog.froggy.repository.RelationshipRepository
import de.whitefrog.froggy.test.Likes
import de.whitefrog.froggy.test.Person
import de.whitefrog.froggy.test.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class TestModels {

  @Test
  fun newInstancesInHashSet() {
    TestSuite.service().beginTx().use {
      var persons: MutableSet<Person> = HashSet()
      persons.add(repository.createModel())
      persons.add(repository.createModel())
      assertThat(persons).hasSize(2)
      repository.save(*persons.toTypedArray())
      persons = HashSet(persons)
      val first = repository.findByUuid(persons.iterator().next().uuid)
      persons.add(first)
      assertThat(persons).hasSize(2)
    }
  }

  @Test
  fun bidirectionalRelationships() {
    TestSuite.service().beginTx().use {
      val man = repository.createModel()
      val woman = repository.createModel()
      repository.save(man, woman)
      man.marriedWith = woman
      repository.save(man)
      repository.fetch(woman, "marriedWith")
      assertThat(woman.marriedWith).isEqualTo(man)
      repository.fetch(man, true, FieldList.parseFields("marriedWith"))
      assertThat(man.marriedWith).isEqualTo(woman)
      // this should not throw an exception and create no additional relationship
      repository.save(woman)
      repository.fetch(woman, "marriedWith")
    }
  }

  companion object {
    private lateinit var repository: PersonRepository
    private lateinit var relationships: RelationshipRepository<Likes>

    @BeforeClass @JvmStatic
    fun before() {
      repository = PersonRepository(TestSuite.service())
      relationships = TestSuite.service().repository(Likes::class.java)
    }
  }
}
