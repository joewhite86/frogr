package de.whitefrog.frogr.benchmark

import de.whitefrog.frogr.test.BaseBenchmark
import de.whitefrog.frogr.test.Benchmark
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.Person
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

@Ignore
class BenchmarkCreate : BaseBenchmark() {
  companion object {
    const val initCount = 5000
    const val count = 50000
  }
  @Test
  @Benchmark(expectation = 200, count = count, timeUnit = TimeUnit.MICROSECONDS)
  fun create() {
    val persons = service().repository(Person::class.java)
    service().beginTx().use { tx ->
      // warmup
      for (i in (count..count+ initCount)) {
        val person = Person("create$i")
        persons.save(person)
      }
      tx.success()
    }
    service().beginTx().use { tx ->
      task().start = System.nanoTime()
      
      for(i in (0..count)) {
        val person = Person("create$i")
        persons.save(person)
      }
      tx.success()
    }
  }

  @Test
  @Benchmark(expectation = 500, count = count, timeUnit = TimeUnit.MICROSECONDS)
  fun createWithRelationship() {
    val persons = service().repository(Person::class.java)
    val clothes = service().repository(Clothing::class.java)
    service().beginTx().use { tx ->

      // warmup
      for (i in (count..initCount+count)) {
        val person = Person("createWithRelationship$i")

        val clothing = Clothing("createWithRelationship$i")
        person.wears.add(clothing)

        clothes.save(clothing)
        persons.save(person)
      }
      tx.success()
    }
    service().beginTx().use { tx ->
      task().start = System.nanoTime()

      for(i in (0..count)) {
        val person = Person("createWithRelationship$i")

        val clothing = Clothing("createWithRelationship$i")
        person.wears.add(clothing)

        clothes.save(clothing)
        persons.save(person)
      }
      tx.success()
    }
  }
}