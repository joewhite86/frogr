package de.whitefrog.frogr

import de.whitefrog.frogr.model.TestFieldList
import de.whitefrog.frogr.model.TestFilters
import de.whitefrog.frogr.model.TestModel
import de.whitefrog.frogr.repository.TestModelRepository
import de.whitefrog.frogr.repository.TestSearch
import de.whitefrog.frogr.rest.request.TestSearchParameterResolver
import de.whitefrog.frogr.rest.response.TestResponse
import de.whitefrog.frogr.test.TemporaryService
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
  TestService::class, 
  TestPersistence::class,
  TestModelRepository::class, 
  TestModel::class, 
  TestSearch::class, 
  TestFieldList::class,
  TestFilters::class,
  TestSearchParameterResolver::class,
  TestResponse::class
)
class TestSuite {
  companion object {
    private val service: Service = TemporaryService()
    fun service(): Service {
      if(!service.isConnected) {
        service.connect()
      }
      return service
    }
  }
}
