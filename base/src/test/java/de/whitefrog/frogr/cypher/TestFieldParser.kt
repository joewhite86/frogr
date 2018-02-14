package de.whitefrog.frogr.cypher

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Clothing
import de.whitefrog.frogr.test.model.InventoryItem
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class TestFieldParser {
  companion object {
    private lateinit var service: Service
    private lateinit var persons: PersonRepository

    @BeforeClass
    @JvmStatic
    fun before() {
      service = TemporaryService()
      service.connect()
      persons = service.repository(Person::class.java)
    }

    @AfterClass
    @JvmStatic
    fun after() {
      service.shutdown()
    }
  }
  
  @Test
  fun parse() {
    val parser = FieldParser(persons)
    val fieldList = parser.parse("likes.likes.wears.name")
    assertThat(fieldList).hasSize(4)
    assertEquals("likes", fieldList[0].name)
    assertEquals(Person::class.java, fieldList[0].baseClass())
    assertEquals("likes", fieldList[1].name)
    assertEquals(Person::class.java, fieldList[1].baseClass())
    assertEquals("wears", fieldList[2].name)
    assertEquals(Clothing::class.java, fieldList[2].baseClass())
    assertEquals("name", fieldList[3].name)
    assertEquals(String::class.java, fieldList[3].baseClass())
  }
  
  @Test
  fun parseInterfaceRelationship() {
    val parser = FieldParser(persons)
    val fieldList = parser.parse("inventory.uuid")
    assertThat(fieldList).hasSize(2)
    assertEquals("inventory", fieldList[0].name)
    assertEquals(InventoryItem::class.java, fieldList[0].baseClass())
    assertEquals("uuid", fieldList[1].name)
    assertEquals(String::class.java, fieldList[1].baseClass())
  }
}