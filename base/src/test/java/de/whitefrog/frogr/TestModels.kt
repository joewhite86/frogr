package de.whitefrog.frogr

import de.whitefrog.frogr.model.rest.FieldList
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class TestModels {
  private var service: Service = TemporaryService()
  private var repository: PersonRepository
  private var relationships: RelationshipRepository<Likes>

  init {
    service.connect()
    repository = service.repository(Person::class.java)
    relationships = service.repository(Likes::class.java)
  }
  
  @Test
  fun newInstancesInHashSet() {
    service.beginTx().use {
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
    service.beginTx().use {
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
}
