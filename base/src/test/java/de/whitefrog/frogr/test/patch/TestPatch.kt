package de.whitefrog.frogr.test.patch

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.patch.Patch
import de.whitefrog.frogr.patch.Version
import de.whitefrog.frogr.test.model.Person

@Version("10.0.0")
class TestPatch(s: Service) : Patch(s) {
  override fun update() {
    service.beginTx().use { tx ->
      val person = Person("test")
      service.repository(Person::class.java).save(person)
      tx.success()
    }
  }
}