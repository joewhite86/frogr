package de.whitefrog.frogr

import de.whitefrog.frogr.test.TemporaryService
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
  TestPersistence::class,
  TestRepositories::class, 
  TestModels::class, 
  TestSearch::class, 
  TestFieldList::class
)
object TestSuite {
  var service: Service = TemporaryService()
  init { 
    service.connect() 
  }
}
