package de.whitefrog.frogr

import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import de.whitefrog.frogr.test.rest.Persons
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.*

class TestApplication {
  companion object {
    val persons: PersonRepository = mock(PersonRepository::class.java)
    @ClassRule
    val resources = ResourceTestRule.builder()
      .addResource(Persons())
      .build()
    val person = Person("test")
  }

  @Before
  fun setup() {
    `when`(persons.search().list<Person>()).thenReturn(mutableListOf(person))
  }
  @After
  fun tearDown() {
    reset(persons)
  }
  
  @Test
  fun read() {
  }
}
