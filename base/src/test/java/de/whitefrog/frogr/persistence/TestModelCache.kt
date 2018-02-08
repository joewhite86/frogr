package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class TestModelCache {
  companion object {
    private lateinit var service: Service

    @BeforeClass @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
    }

    @AfterClass @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test(expected = NoSuchFieldException::class)
  fun getUnknownField() {
    ModelCache.getField(Person::class.java, "unknownField")
  }
  @Test
  fun containsModel() {
    assertTrue(service.persistence().cache().containsModel("Person"))
    assertFalse(service.persistence().cache().containsModel("UnknownModel"))
  }
  @Test
  fun fieldDescriptorByField() {
    val field = Person::class.java.getDeclaredField("field")
    val descriptor = service.persistence().cache().fieldDescriptor(field)
    assertNotNull(descriptor)
    assertThat(descriptor.field()).isEqualTo(field)
  }
  @Test
  fun subtypesOf() {
    val subtypes = service.persistence().cache().subTypesOf(Model::class.java)
    assertThat(subtypes).isNotEmpty
    assertThat(subtypes).contains(Person::class.java)
  }
}