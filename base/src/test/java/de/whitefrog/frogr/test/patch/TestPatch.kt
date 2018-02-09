package de.whitefrog.frogr.test.patch

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.patch.Patch
import de.whitefrog.frogr.patch.Version
import de.whitefrog.frogr.test.model.Person

@Version("9.9.99")
class TestPatch(s: Service) : Patch(s) {
  override fun update() {
    service.beginTx().use { tx ->
      val person = Person("patch1")
      service.repository(Person::class.java).save(person)
      tx.success()
    }
  }
}