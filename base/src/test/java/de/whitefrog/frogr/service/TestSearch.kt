package de.whitefrog.frogr.service

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.model.Filter
import de.whitefrog.frogr.model.SearchParameter
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


class TestSearch {
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

  private fun prepareData(): List<Person> {
    val person1 = persons.createModel()
    person1.field = "test1"
    person1.uniqueField = "test1"
    person1.lowerCaseIndex = "test1"
    person1.number = 10L

    val person2 = persons.createModel()
    person2.field = "test2"
    person2.uniqueField = "test2"
    person2.lowerCaseIndex = "test2"
    person2.number = 20L

    persons.save(person1, person2)

    val likes1 = Likes(person1, person2)
    val likes2 = Likes(person2, person1)

    likesRepository.save(likes1, likes2)
    
    person1.likes.add(person2)
    person2.likes.add(person1)

    return Arrays.asList(person1, person2)
  }
  
  @Test
  fun startsWith() {
    service.beginTx().use {
      prepareData()
      val results = persons.search()
        .filter(Filter.StartsWith("field", "test"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }
  
  @Test
  fun endsWith() {
    service.beginTx().use {
      prepareData()
      val results = persons.search()
        .filter(Filter.EndsWith("field", "t1"))
        .list<Person>()
      assertThat(results).hasSize(1)
    }
  }

  @Test
  fun contains() {
    service.beginTx().use {
      prepareData()
      val results = persons.search()
        .filter(Filter.Contains("field", "est"))
        .list<Person>()
      assertThat(results).hasSize(2)
    }
  }

  @Test
  fun ids() {
    service.beginTx().use {
      val list = prepareData()
      val person = persons.search()
        .ids(list[0].id)
        .single<Person>()
      assertThat(person).isEqualTo(list[0])
    }
  }

  @Test
  fun uuids() {
    service.beginTx().use {
      val list = prepareData()
      val person = persons.search()
        .uuids(list[0].uuid)
        .single<Person>()
      assertThat(person).isEqualTo(list[0])
    }
  }

  @Test
  fun enumValue() {
    service.beginTx().use {
      val person = Person()
      person.age = Person.Age.Old
      persons.save(person)
      val found: Person = persons.search().filter(Filter.Equals("age", "Old")).single()
      assertThat(found).isEqualTo(person)
    }
  }

  @Test
  fun dateValue() {
    service.beginTx().use {
      val person = Person()
      val date = Date()
      person.dateField = date
      persons.save(person)
      val found: Person = persons.search().filter(Filter.Equals("dateField", date)).single()
      assertThat(found).isEqualTo(person)
    }
  }
  
  @Test
  fun dateGreaterThan() {
    service.beginTx().use {
      val person = Person()
      val date = Date()
      person.dateField = date
      persons.save(person)
      val cal = Calendar.getInstance()
      cal.time = date
      cal.add(Calendar.DAY_OF_MONTH, -1)
      val found: Person = persons.search().filter(Filter.GreaterThan("dateField", cal.time)).single()
      assertThat(found).isEqualTo(person)

// throws nullpointerexception in kotlin
//      cal.time = date
//      cal.add(Calendar.DAY_OF_MONTH, 1)
//      found = persons.search().filter(Filter.GreaterThan("dateField", cal.time)).single()
//      assertThat(found).isNull()
    }
  }

  @Test
  fun dateLessThan() {
    service.beginTx().use {
      val person = Person()
      val date = Date()
      person.dateField = date
      persons.save(person)
      val cal = Calendar.getInstance()
      cal.time = date
      cal.add(Calendar.DAY_OF_MONTH, 1)
      val found: Person = persons.search().filter(Filter.LessThan("dateField", cal.time)).single()
      assertThat(found).isEqualTo(person)
      
// throws nullpointerexception in kotlin
//      cal.time = date
//      cal.add(Calendar.DAY_OF_MONTH, -1)
//      found = persons.search().filter(Filter.LessThan("dateField", cal.time)).single()
//      assertThat(found).isNull()
    }
  }

  @Test
  fun count() {
    service.beginTx().use {
      prepareData()
      val count = persons.search().count()
      assertThat(count).isEqualTo(2)
    }
  }

  @Test
  fun sum() {
    service.beginTx().use {
      val list = prepareData()
      val sum = persons.search().sum("person.number").toLong()
      assertThat(sum).isEqualTo(list[0].number!! + list[1].number!!)
    }
  }
  
  @Test
  fun set() {
    service.beginTx().use {
      val list = prepareData()
      val result = persons.search().set<Person>()
      assertThat(result).isInstanceOfAny(Set::class.java)
      assertThat(result).hasSize(list.size)
    }
  }

  @Test
  fun list() {
    service.beginTx().use {
      val list = prepareData()
      val result = persons.search().list<Person>()
      assertThat(result).isInstanceOfAny(List::class.java)
      assertThat(result).hasSize(list.size)
    }
  }

  @Test
  fun toLong() {
    service.beginTx().use {
      prepareData()
      val long = persons.search().limit(1).returns("person.number").toLong()
      assertThat(long is Long).isTrue()
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toLongTooManyReturns() {
    service.beginTx().use {
      prepareData()
      persons.search().limit(1).returns("person.number", "person.field").toLong()
    }
  }

  @Test
  fun toInt() {
    service.beginTx().use {
      prepareData()
      val long = persons.search().limit(1).returns("person.number").toInt()
      assertThat(long is Int).isTrue()
    }
  }

  @Test(expected = UnsupportedOperationException::class)
  fun toIntTooManyReturns() {
    service.beginTx().use {
      prepareData()
      persons.search().limit(1).returns("person.number", "person.field").toInt()
    }
  }
  
  @Test 
  fun fulltext() {
    service.beginTx().use {
      val list = prepareData()
      val result = persons.search()
        .filter(Filter.Equals("lowerCaseIndex", "TEST1"))
        .list<Person>()
      assertThat(result).isNotEmpty
      assertThat(result[0]).isEqualTo(list[0])
    }
  }
  
  @Test
  fun searchLikedPersons() {
    service.beginTx().use {
      val list = prepareData()
      val result = persons.search()
        .filter(Filter.GreaterThan("likes.number", 10L))
        .returns("likes")
        .list<Person>()
      assertThat(result).isNotEmpty
      assertThat(result[0]).isEqualTo(list[1])
    }
  }
  
  @Test
  fun start() {
    service.beginTx().use {
      prepareData()
      val results = persons.search().list<Person>()
      val result = persons.search().start(1).limit(1).single<Person>()
      assertThat(results[1]).isEqualTo(result)
    }
  }
  
  @Test
  fun query() {
    service.beginTx().use { 
      prepareData()
      val results = persons.search().query("test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }

  @Test
  fun queryConcreteField() {
    service.beginTx().use {
      prepareData()
      val results = persons.search().query("field:test*").list<Person>()
      assertThat(results).isNotEmpty
    }
  }
  
  @Test
  fun paging() {
    service.beginTx().use { 
      prepareData()
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
      prepareData()
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
  
  @Test
  fun returnRelated() {
    service.beginTx().use {
      prepareData()
      val results = persons.search()
        .returns("person", "likes")
        .list<Person>()
      assertThat(results[0].likes).isNotEmpty
    }
  }
}
