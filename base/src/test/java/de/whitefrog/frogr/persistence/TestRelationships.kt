package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.model.FieldList
import de.whitefrog.frogr.model.QueryField
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.*

class TestRelationships {
  private lateinit var service: Service
  private lateinit var persons: PersonRepository
  private lateinit var likesRepository: RelationshipRepository<Likes>
  private lateinit var persistence: Persistence

  @Before
  fun before() {
    service = TemporaryService()
    service.connect()
    persistence = service.persistence()
    persons = service.repository(Person::class.java)
    likesRepository = service.repository(Likes::class.java)
  }
  @After
  fun after() {
    service.shutdown()
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
      val likes = persistence.relationships().getRelationships<Likes>(person1, descriptor, 
        QueryField("likesRelationships"), FieldList.parseFields("to.field"))
      assertNotNull(likes)
      assertThat(likes).hasSize(2)
      assertThat(likes.elementAt(0)!!.to.field).isEqualTo("test2")
    }
  }
  @Test
  fun removeProperty() {
    service.beginTx().use {
      val person1 = Person("test1")
      val person2 = Person("test2")
      persons.save(person1, person2)
      
      val likes = Likes(person1, person2)
      likes.field = "testLike"
      likesRepository.save(likes)
      
      likes.removeProperty("field")
      likesRepository.save(likes)
      val found = likesRepository.find(likes.id, "field")
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
}