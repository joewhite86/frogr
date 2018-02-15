package de.whitefrog.frogr.benchmark

import de.whitefrog.frogr.test.BaseBenchmark
import de.whitefrog.frogr.test.Benchmark
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Person
import org.junit.*
import java.util.*
import java.util.concurrent.TimeUnit

@Ignore
class BenchmarkSearch : BaseBenchmark() {
  companion object {
    const val initPersons = 100000
    const val searchCount = 200000
    
    private val personList = ArrayList<Person>(initPersons)
    
    @BeforeClass @JvmStatic
    fun beforeClass() {
      println("initializing data")
      val persons = service().repository(Person::class.java)
      val clothes = service().repository(Clothing::class.java)
      var tx = service().beginTx()
      personList.clear()
      for(i in (0..initPersons)) {
        val person = Person("person$i")
        if(i > 0) person.likes.add(personList.last())

        val clothing = Clothing("clothing$i")
        person.wears.add(clothing)

        clothes.save(clothing)
        persons.save(person)
        personList.add(person)
        if(i > 0 && i % 50000 == 0) {
          tx.success()
          tx.close()
          println("$i entries created")
          tx = service().beginTx()
        }
      }
      println("benchmark prepared")
    }
  }
  
  @Test
  @Benchmark(expectation = 80, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun search() {
    service().beginTx().use {
      val persons = service().repository(Person::class.java)
      
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
  fun searchWithRelationship() {
    service().beginTx().use {
      val persons = service().repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand")
          .fields("clothing")
          .single<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("field", "person$rand")
          .fields("clothing")
          .single<Person>()
      }
    }
  }
  @Test
  @Benchmark(expectation = 80, count = searchCount, timeUnit = TimeUnit.MICROSECONDS)
  fun searchByRelationshipUuid() {
    service().beginTx().use {
      val persons = service().repository(Person::class.java)

      // warmup
      for(i in (0..2000)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("wears.uuid", personList[rand.toInt()].wears[0].uuid)
          .fields("clothing")
          .list<Person>()
      }

      task().start = System.nanoTime()

      for(i in (0..searchCount)) {
        val rand = Math.floor(Math.random() * initPersons).toLong()
        persons.search().filter("wears.uuid", personList[rand.toInt()].wears[0].uuid)
          .fields("clothing")
          .list<Person>()
      }
    }
  }
}