package de.whitefrog.frogr.repository

import de.whitefrog.frogr.TestSuite
import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.exception.FieldNotFoundException
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.model.Filter
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.model.PersonRequiredField
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.util.*

class TestModelRepository {
  companion object {
    private var service = TestSuite.service
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
  fun correctLabel() {
    assertThat(persons.label().name()).isEqualTo("Person")
  }

  @Test
  fun createModel() {
    service.beginTx().use {
      var model = persons.createModel()
      model.field = "test"
      persons.save(model)
      model = persons.find(model.id, "field")
      assertThat(model).isNotNull()
      assertThat(model.field).isEqualTo("test")
    }
  }

  @Test(expected = MissingRequiredException::class)
  fun missingRequiredField() {
    service.beginTx().use {
      val repository = service.repository<ModelRepository<PersonRequiredField>, PersonRequiredField>(PersonRequiredField::class.java)
      val model = repository.createModel()
      repository.save(model)
    }
  }
  
  @Test
  fun removeProperty() {
    service.beginTx().use {
      val person = Person()
      person.field = "test"
      persons.save(person)
      assertThat(persons.find(person.id, "field").field).isEqualTo(person.field)
      person.removeProperty("field")
      persons.save(person)
      assertThat(persons.find(person.id, "field").field).isNull()
    }
  }
  
  @Test
  fun nullRemoveProperty() {
    service.beginTx().use {
      val person = Person()
      person.nullRemoveField = "test"
      persons.save(person)
      person.nullRemoveField = null
      persons.save(person)
      var found = persons.find(person.id, "nullRemoveField")
      assertThat(found.nullRemoveField).isNull()
      found= persons.search().filter(Filter.Equals("nullRemoveField", "test")).single()
      assertThat(found).isNull()
    }
  }

  @Test(expected = FieldNotFoundException::class)
  fun removeNotExistingProperty() {
    service.beginTx().use {
      val person = Person()
      persons.save(person)
      person.removeProperty("field_xy")
      persons.save(person)
    }
  }
  
  @Test
  fun delete() {
    service.beginTx().use {
      val person = Person()
      persons.save(person)
      assertThat(persons.find(person.id)).isEqualTo(person)
      persons.remove(person)
      assertThat(persons.find(person.id)).isNull()
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

  @Test
  fun uuid() {
    service.beginTx().use {
      val model = persons.createModel()
      persons.save(model)
      assertThat(model.uuid).isNotEmpty()
    }
  }

  @Test(expected = DuplicateEntryException::class)
  fun uniqueConstraint() {
    service.beginTx().use {
      val model = persons.createModel()
      model.uniqueField = "unique"
      persons.save(model)
      val duplicate = persons.createModel()
      duplicate.uniqueField = "unique"
      persons.save(duplicate)
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
      assertThat(likes).isNotNull()
      assertThat(likes.field).isEqualTo("test")
      assertThat(likes.from).isEqualTo(model1)
      assertThat(likes.from.field).isEqualTo("test1")
      assertThat(likes.to).isEqualTo(model2)
      assertThat(likes.to.field).isEqualTo("test2")
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
      assertThat(model1).isNotNull()
      assertThat(model1.likes).hasSize(1)
      assertThat(model1.likes[0]).isEqualTo(model2)
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

  @Test
  fun findByUuid() {
    service.beginTx().use {
      val person = persons.createModel()
      persons.save(person)
      assertThat(person.uuid).isNotEmpty()
      val found = persons.findByUuid(person.uuid)
      assertThat(found).isEqualTo(person)
    }
  }

//  @Test(expected = RepositoryInstantiationException::class)
//  fun invalidRepository() {
//    service.repository("Invalid")
//  }

  @Test
  fun defaultRepository() {
    val repository = service.repository(Clothing::class.java)
    assertThat(repository).isInstanceOfAny(DefaultRepository::class.java)
    assertThat(service.repositoryFactory().cache()).contains(repository)
  }

  @Test
  fun repositoryCache() {
    assertThat(service.repositoryFactory().cache()).contains(likesRepository)
  }
}
