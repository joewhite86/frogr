package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.RelatedNotPersistedException
import de.whitefrog.frogr.model.FieldList
import de.whitefrog.frogr.model.QueryField
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.MarriedWith
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.RelationshipType
import java.util.*

class TestRelationships {
  companion object {
    private lateinit var service: Service
    private lateinit var persons: PersonRepository
    private lateinit var likesRepository: RelationshipRepository<Likes>
    private lateinit var persistence: Persistence

    @BeforeClass @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
      persistence = service.persistence()
      persons = service.repository(Person::class.java)
      likesRepository = service.repository(Likes::class.java)
    }

    @AfterClass @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test
  fun getRelationships() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      
      person1.likes.addAll(Arrays.asList(person2, person3))
      persons.save(person2, person3, person1)
      
      val descriptor = persistence.cache().fieldDescriptor(Person::class.java, "likesRelationships")
      val likes = persistence.relationships().getRelationships<Likes>(person1, descriptor!!, 
        QueryField("likesRelationships"), FieldList.parseFields("to.field"))
      assertNotNull(likes)
      assertThat(likes).hasSize(2)
      assertThat(likes.elementAt(0).to.field).isEqualTo("test2")
    }
  }
  @Test
  fun removeProperty() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1, person2)
      
      val likes = Likes(person1, person2, "testLike")
      likesRepository.save(likes)
      
      likes.removeProperty("field")
      likesRepository.save(likes)
      val found = likesRepository.find(likes.id, "field")
      assertNotNull(found)
      assertNull(found.field)
    }
  }
  @Test
  fun delete() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      persons.save(person1, person2, person3)
      likesRepository.save(Likes(person1, person2), Likes(person1, person3))
      
      persons.fetch(person1, "likes", "likesRelationships")
      assertThat(person1.likes).hasSize(2)
      assertThat(person1.likesRelationships).hasSize(2)
      
      likesRepository.remove(person1.likesRelationships[0])
      assertNull(likesRepository.find(person1.likesRelationships[0].id))
      
      persons.refetch(person1, "likes", "likesRelationships")
      assertThat(person1.likes).hasSize(1)
      assertThat(person1.likesRelationships).hasSize(1)
    }  
  }
  @Test
  fun deleteRelationshipBetween() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      person1.likes.addAll(mutableListOf(person2, person3))
      persons.save(person3, person2, person1)
      
      persistence.relationships().delete(person1, RelationshipType.withName("Likes"), Direction.OUTGOING, person2)
      
      persons.refetch(person1, "likes")
      assertThat(person1.likes).hasSize(1)
      
      person1.resetId()
      person3.resetId()

      persistence.relationships().delete(person1, RelationshipType.withName("Likes"), Direction.OUTGOING, person3)

      persons.refetch(person1, "likes")
      assertThat(person1.likes).isEmpty()
    }
  }
  @Test
  fun relationshipCountField() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      person1.likes.add(person2)
      persons.save(person3, person2, person1)
      
      persons.fetch(person1, "likesCount")
      assertThat(person1.likesCount).isEqualTo(1)
      person1.likes.add(person3)
      persons.save(person1)
      persons.refetch(person1, FieldList.parseFields("likesCount"))
      assertThat(person1.likesCount).isEqualTo(2)
    }
  }
  @Test
  fun saveModelWithRelationshipModels() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      persons.save(person1, person2, person3)
      
      person1.likesRelationships.add(Likes(person1, person2))
      person1.likesRelationships.add(Likes(person1, person3))
      persons.save(person1)
      
      persons.fetch(person1, "likes")
      assertThat(person1.likes).hasSize(2)
    }
  }
  @Test
  fun saveModelWithIncomingRelationshipModels() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      val person3 = Person("test3")
      persons.save(person1, person2, person3)

      person2.likedByRelationships.add(Likes(person1, person2))
      person3.likedByRelationships.add(Likes(person1, person3))
      persons.save(person2, person3)

      persons.fetch(person1, "likes")
      assertThat(person1.likes).hasSize(2)
    }
  }
  @Test
  fun saveModelWithBothDirectionRelationshipModels() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1, person2)

      person1.marriedWithRelationship = MarriedWith(person1, person2)
      persons.save(person1, person2)

      persons.fetch(person1, "marriedWith")
      assertThat(person1.marriedWith).isEqualTo(person2)
    }
  }
  @Test(expected = RelatedNotPersistedException::class)
  fun relationshipModelNotPersisted() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1)

      person1.likesRelationships.add(Likes(person1, person2))
      persons.save(person1)
    }
  }
  @Test
  fun getRelationshipBetween() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1, person2)
      
      person1.likes.add(person2)
      persons.save(person1)
      
      val relationship = persistence.relationships()
        .getRelationshipBetween<Likes>(person1, person2, RelationshipType.withName("Likes"), Direction.OUTGOING)
      assertNotNull(relationship)
    }
  }
  @Test
  fun getRelationshipBetween_NotExisting() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1, person2)

      val relationship = persistence.relationships()
        .getRelationshipBetween<Likes>(person1, person2, RelationshipType.withName("Likes"), Direction.OUTGOING)
      assertNull(relationship)
    }
  }
  @Test
  fun nonLazyRelationships() {
    service.beginTx().use {
      val person = Person("test1")
      val jacket = Clothing("Jacket")
      val trousers = Clothing("Trousers")
      service.repository(Clothing::class.java).save(jacket, trousers)
      person.wears.addAll(Arrays.asList(jacket, trousers))
      persons.save(person)
      
      persons.refetch(person, "wears")
      assertThat(person.wears).hasSize(2)
      
      person.wears.remove(jacket)
      persons.save(person)
      
      persons.refetch(person, "wears")
      assertThat(person.wears).hasSize(1)
    }
  }
}