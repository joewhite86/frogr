package de.whitefrog.frogr.test.patch

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.patch.Patch
import de.whitefrog.frogr.patch.Version
import de.whitefrog.frogr.test.model.Person

@Version(value = "10.0.0", proority = 1)
class TestPatch3(s: Service) : Patch(s) {
  override fun update() {
    service.beginTx().use { tx ->
      val repository = service.repository(Person::class.java)
      if(repository.search().filter("field", "patch1").single<Person>() == null)
        throw FrogrException()
      val person = Person("patch3")
      repository.save(person)
      tx.success()
    }
  }
}