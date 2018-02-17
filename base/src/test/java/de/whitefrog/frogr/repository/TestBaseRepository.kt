package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.DuplicateEntryException
import de.whitefrog.frogr.exception.FieldNotFoundException
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.model.Filter
import de.whitefrog.frogr.model.SearchParameter
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.LightweightPerson
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.model.PersonRequiredField
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class TestBaseRepository {
  companion object {
    private lateinit var service: Service
    private lateinit var persons: PersonRepository

    @BeforeClass @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
      persons = service.repository(Person::class.java)
    }

    @AfterClass @JvmStatic
    fun after() {
      service.shutdown()
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
  fun findByUuid() {
    service.beginTx().use {
      val person = persons.createModel()
      persons.save(person)
      assertThat(person.uuid).isNotEmpty()
      val found = persons.findByUuid(person.uuid)
      assertThat(found).isEqualTo(person)
    }
  }
  
  @Test
  fun findLightweightModel() {
    service.beginTx().use {
      val repository = service.repository(LightweightPerson::class.java)
      val person = LightweightPerson("test")
      repository.save(person)
      
      var found = repository.search()
        .filter("field", "test")
        .fields("field")
        .single<LightweightPerson>()
      
      assertEquals(person, found)
      assertEquals(person.field, found.field)
      
      found = repository.search()
        .ids(person.id)
        .fields("field")
        .single()

      assertEquals(person, found)
      assertEquals(person.field, found.field)
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
      assertNull(persons.find(person.id, "field").field)
    }
  }

  @Test
  fun repositoryCache() {
    assertThat(service.repositoryFactory().cache()).contains(persons)
  }
  
  @Test
  fun contains() {
    service.beginTx().use {
      val person = Person("test")
      persons.save(person)
      assertTrue(persons.contains(person))
    }
  }
  
  @Test
  fun persistenceIsSet() {
    assertNotNull(persons.persistence())
  }
  
  @Test
  fun relationshipsIsSet() {
    assertNotNull(persons.relationships())
  }
  
  @Test(expected = FrogrException::class)
  fun notPersistedInFetch() {
    service.beginTx().use {
      val person = Person()
      persons.fetch(person, "field")
    }
  }
  
  @Test(expected = IllegalArgumentException::class)
  fun wrongModelInFetch() {
    service.beginTx().use {
      val clothing = Clothing()
      persons.fetch(clothing, "field")
    }
  }
  
  @Test
  fun sort() {
    val person1 = Person("test1", 1)
    val person2 = Person("test2", 2)
    val person3 = Person("test3", 3)
    val person4 = Person("test3", 4)
    val list = mutableListOf(person1, person2, person3, person4)
    var orderBy = mutableListOf(SearchParameter.OrderBy("field", SearchParameter.SortOrder.DESC))
    persons.sort(list, orderBy)
    var previous : Person? = null
    for(person in list) {
      if(previous == null) {
        previous = person
        continue
      }
      assertThat(person.field!!.compareTo(previous.field!!)).isLessThanOrEqualTo(0)
    }

    orderBy = mutableListOf(SearchParameter.OrderBy("field", SearchParameter.SortOrder.ASC))
    persons.sort(list, orderBy)
    previous = null
    for(person in list) {
      if(previous == null) {
        previous = person
        continue
      }
      assertThat(person.field!!.compareTo(previous.field!!)).isGreaterThanOrEqualTo(0)
    }

    orderBy = mutableListOf(
      SearchParameter.OrderBy("field", SearchParameter.SortOrder.DESC),
      SearchParameter.OrderBy("number", SearchParameter.SortOrder.DESC)
    )
    persons.sort(list, orderBy)
    previous = null
    for(person in list) {
      if(previous == null) {
        previous = person
        continue
      }
      assertThat(person.field!!.compareTo(previous.field!!)).isLessThanOrEqualTo(0)
      assertThat(person.number!!.compareTo(previous.number!!)).isLessThan(0)
    }
  } 
}