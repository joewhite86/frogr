package de.whitefrog.frogr

import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.util.*
import kotlin.collections.ArrayList

@RunWith(Suite::class)
@Suite.SuiteClasses(
  TestService::class, 
  TestRepositories::class, 
  TestModels::class, 
  TestSearch::class, 
  TestFieldList::class
)
object TestSuite {
  fun prepareData(service: Service): List<Person> {
    val persons = service.repository<PersonRepository, Person>(Person::class.java)
    val likesRepository = service.repository(Likes::class.java)
    var list: List<Person> = ArrayList()

    service.beginTx().use { tx ->
      val person1 = persons.createModel()
      person1.field = "test1"
      person1.uniqueField = "test1"
      person1.number = 10L

      val person2 = persons.createModel()
      person2.field = "test2"
      person2.uniqueField = "test2"
      person2.number = 20L

      persons.save(person1, person2)

      val likes1 = Likes(person1, person2)
      val likes2 = Likes(person2, person1)

      likesRepository.save(likes1, likes2)

      list = Arrays.asList(person1, person2)

      tx.success()
    }

    return list
  }
}
