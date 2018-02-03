package de.whitefrog.frogr

import de.whitefrog.frogr.model.rest.Filter
import de.whitefrog.frogr.model.rest.SearchParameter
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestSearch {
  private val service: Service = TemporaryService()
  private var persons: PersonRepository
  private var likesRepository: RelationshipRepository<Likes>
  private var person1: Person
  private var person2: Person

  init {
    service.connect()
    val list = TestSuite.prepareData(service)
    person1 = list[0]
    person2 = list[1]
    persons = service.repository(Person::class.java)
    likesRepository = service.repository(Likes::class.java)
  }
  @Test
  fun startsWith() {
    service.beginTx().use {
      val results = persons.search()
        .filter(Filter.StartsWith("field", "test"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }
  
  @Test
  fun endsWith() {
    service.beginTx().use {
      val results = persons.search()
        .filter(Filter.EndsWith("field", "t1"))
        .list<Person>()
      assertThat(results).hasSize(1)
    }
  }

  @Test
  fun contains() {
    service.beginTx().use {
      val results = persons.search()
        .filter(Filter.Contains("field", "est"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }

  @Test
  fun ids() {
    service.beginTx().use {
      val person = persons.search()
        .ids(person1.id)
        .single<Person>()
      assertThat(person).isEqualTo(person1)
    }
  }

  @Test
  fun uuids() {
    service.beginTx().use {
      val person = persons.search()
        .uuids(person1.uuid)
        .single<Person>()
      assertThat(person).isEqualTo(person1)
    }
  }

  @Test
  fun count() {
    service.beginTx().use {
      val count = persons.search().count()
      assertThat(count).isEqualTo(2)
    }
  }

  @Test
  fun sum() {
    service.beginTx().use {
      val sum = persons.search().sum("person.number").toLong()
      assertThat(sum).isEqualTo(person1.number!! + person2.number!!)
    }
  }
  
  @Test
  fun set() {
    service.beginTx().use { 
      val set = persons.search().set<Person>()
      assertThat(set).isInstanceOfAny(Set::class.java)
    }
  }

  @Test
  fun toLong() {
    service.beginTx().use {
      val long = persons.search().limit(1).returns("person.number").toLong()
      assertThat(long is Long).isTrue
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toLongTooManyReturns() {
    service.beginTx().use {
      persons.search().limit(1).returns("person.number", "person.field").toLong()
    }
  }

  @Test
  fun toInt() {
    service.beginTx().use {
      val long = persons.search().limit(1).returns("person.number").toInt()
      assertThat(long is Int).isTrue
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toIntTooManyReturns() {
    service.beginTx().use {
      persons.search().limit(1).returns("person.number", "person.field").toInt()
    }
  }
  
  @Test
  fun searchLikedPersons() {
    service.beginTx().use { 
      val result = persons.search()
        .filter(Filter.GreaterThan("likes.number", 0L))
        .returns("likes")
        .list<Person>()
      assertThat(result).isNotEmpty
    }
  }
  
  @Test
  fun start() {
    service.beginTx().use { 
      val results = persons.search().list<Person>()
      val result = persons.search().start(1).limit(1).single<Person>()
      assertThat(results[1]).isEqualTo(result)
    }
  }
  
  @Test
  fun query() {
    service.beginTx().use { 
      val results = persons.search().query("test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }

  @Test
  fun queryConcreteField() {
    service.beginTx().use {
      val results = persons.search().query("uniqueField:test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }
  
  @Test
  fun paging() {
    service.beginTx().use { 
      val results = persons.search().list<Person>()
      var page = persons.search().limit(1).list<Person>()
      assertThat(page).hasSize(1)
      assertThat(page[0]).isEqualTo(results[0])
      page = persons.search().limit(1).page(2).list()
      assertThat(page).hasSize(1)
      assertThat(page[0]).isEqualTo(results[1])
    }
  }
  
  @Test
  fun orderBy() {
    service.beginTx().use { 
      var results = persons.search()
        .orderBy("number", SearchParameter.SortOrder.ASC)
        .fields("number")
        .list<Person>()
      var prev: Person? = null
      for(result in results) {
        if(prev != null) {
          assertThat(result.number).isGreaterThan(prev.number)
        }
        prev = result
      }
      
      prev = null
      results = persons.search()
        .orderBy("number", SearchParameter.SortOrder.DESC)
        .fields("number")
        .list()
      for(result in results) {
        if(prev != null) {
          assertThat(result.number).isLessThan(prev.number)
        }
        prev = result
      }
    }
  }
}
