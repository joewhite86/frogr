package de.whitefrog.froggy

import de.whitefrog.froggy.model.rest.Filter
import de.whitefrog.froggy.repository.RelationshipRepository
import de.whitefrog.froggy.test.Likes
import de.whitefrog.froggy.test.Person
import de.whitefrog.froggy.test.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class TestSearch {
  @Test
  fun startsWith() {
    TestSuite.service().beginTx().use {
      val results = persons.search()
        .filter(Filter.StartsWith("field", "test"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }
  
  @Test
  fun endsWith() {
    TestSuite.service().beginTx().use {
      val results = persons.search()
        .filter(Filter.EndsWith("field", "t1"))
        .list<Person>()
      assertThat(results).hasSize(1)
    }
  }

  @Test
  fun contains() {
    TestSuite.service().beginTx().use {
      val results = persons.search()
        .filter(Filter.Contains("field", "est"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }

  @Test
  fun ids() {
    TestSuite.service().beginTx().use {
      val person = persons.search()
        .ids(TestSuite.person1.id)
        .single<Person>()
      assertThat(person).isEqualTo(TestSuite.person1)
    }
  }

  @Test
  fun uuids() {
    TestSuite.service().beginTx().use {
      val person = persons.search()
        .uuids(TestSuite.person1.uuid)
        .single<Person>()
      assertThat(person).isEqualTo(TestSuite.person1)
    }
  }

  @Test
  fun count() {
    TestSuite.service().beginTx().use {
      val count = persons.search().count()
      assertThat(count).isEqualTo(2)
    }
  }

  @Test
  fun sum() {
    TestSuite.service().beginTx().use {
      val sum = persons.search().sum("person.number").toLong()
      assertThat(sum).isEqualTo(TestSuite.person1.number!! + TestSuite.person2.number!!)
    }
  }
  
  @Test
  fun set() {
    TestSuite.service().beginTx().use { 
      val set = persons.search().set<Person>()
      assertThat(set).isInstanceOfAny(Set::class.java)
    }
  }

  @Test
  fun toLong() {
    TestSuite.service().beginTx().use {
      val long = persons.search().limit(1).returns("person.number").toLong()
      assertThat(long is Long).isTrue
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toLongTooManyReturns() {
    TestSuite.service().beginTx().use {
      persons.search().limit(1).returns("person.number", "person.field").toLong()
    }
  }

  @Test
  fun toInt() {
    TestSuite.service().beginTx().use {
      val long = persons.search().limit(1).returns("person.number").toInt()
      assertThat(long is Int).isTrue
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toIntTooManyReturns() {
    TestSuite.service().beginTx().use {
      persons.search().limit(1).returns("person.number", "person.field").toInt()
    }
  }
  
  @Test
  fun searchLikedPersons() {
    TestSuite.service().beginTx().use { 
      val result = persons.search()
        .filter(Filter.GreaterThan("likes.number", 0L))
        .returns("likes")
        .list<Person>()
      assertThat(result).isNotEmpty
    }
  }
  
  @Test
  fun start() {
    TestSuite.service().beginTx().use { 
      val results = persons.search().list<Person>()
      val result = persons.search().start(1).limit(1).single<Person>()
      assertThat(results[1]).isEqualTo(result)
    }
  }
  
  @Test
  fun query() {
    TestSuite.service().beginTx().use { 
      val results = persons.search().query("test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }

  @Test
  fun queryConcreteField() {
    TestSuite.service().beginTx().use {
      val results = persons.search().query("uniqueField:test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }

  companion object {
    lateinit var persons: PersonRepository
    lateinit var likesRepository: RelationshipRepository<Likes>

    @BeforeClass @JvmStatic
    fun before() {
      persons = TestSuite.service().repository(Person::class.java)
      likesRepository = TestSuite.service().repository(Likes::class.java)
    }
  }
}
