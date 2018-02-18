package de.whitefrog.frogr.benchmark

import de.whitefrog.frogr.test.BaseBenchmark
import de.whitefrog.frogr.test.Benchmark
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Person
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

@Ignore
class BenchmarkSearch : BaseBenchmark() {
  companion object {
    private val service = TemporaryService()
    const val initPersons = 50000
    const val searchCount = 250000
    
    private val personList = ArrayList<Person>(initPersons)
    
    @BeforeClass @JvmStatic
    fun beforeClass() {
      println("initializing data")
      service.connect()
      val persons = service.repository(Person::class.java)
      val clothes = service.repository(Clothing::class.java)
      var tx = service.beginTx()
      personList.clear()
      for(i in (0..initPersons)) {
        val person = Person("person$i")
        person.uniqueField = "person_unique$i"
        person.secureField = "person_secure$i"
        person.autoFetch = "person_autofetch$i"
        person.number = 100L + i

        if(i > 2) {
          var rand = Math.floor(Math.random() * personList.size).toLong()
          person.likes.add(personList[rand.toInt()])
          rand = Math.floor(Math.random() * personList.size).toLong()
          person.likes.add(personList[rand.toInt()])
        }

        person.wears.add(Clothing("clothing$i"))
        person.wears.add(Clothing("clothing${initPersons+i+1}"))

        clothes.save(*person.wears.toTypedArray())
        persons.save(person)
        personList.add(person)
        if(i > 0 && i % 50000 == 0) {
          tx.success()
          tx.close()
          println("$i entries created")
          tx = service.beginTx()
        }
      }
      tx.success()
      tx.close()
      println("benchmark prepared")
    }
    
    @AfterClass @JvmStatic
    fun shutdown() {
      service.shutdown()
    }
  }

  @Test
  @Benchmark(expectation = 50, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchById() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().ids(personList[rand.toInt()].id).single<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().ids(personList[rand.toInt()].id).single<Person>()
      }
    }
  }

  @Test
  @Benchmark(expectation = 50, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchByIdFetchAll() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search()
          .ids(personList[rand.toInt()].id)
          .fields("all")
          .single<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search()
          .ids(personList[rand.toInt()].id)
          .fields("all")
          .single<Person>()
      }
    }
  }
  
  @Test
  @Benchmark(expectation = 80, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchByField() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)
      
      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand").single<Person>()
      }
      
      task().start = System.nanoTime()
      
      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand").single<Person>()
      }
    }
  }

  @Test
  @Benchmark(expectation = 80, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchByFieldWithRelationships() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand")
          .fields("all,wears,likes.all")
          .single<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand")
          .fields("all,wears,likes.all")
          .single<Person>()
      }
    }
  }
  @Test
  @Benchmark(expectation = 80, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchByRelationshipUuid() {
    service.beginTx().use {
      val persons = service.repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("wears.uuid", personList[rand.toInt()].wears[0].uuid)
          .fields("all,wears,likes.all")
          .list<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("wears.uuid", personList[rand.toInt()].wears[0].uuid)
          .fields("all,wears,likes.all")
          .list<Person>()
      }
    }
  }
}