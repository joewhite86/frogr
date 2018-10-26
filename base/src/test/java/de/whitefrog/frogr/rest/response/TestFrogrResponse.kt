package de.whitefrog.frogr.rest.response

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.whitefrog.frogr.test.model.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestFrogrResponse {
  @Test
  fun serialize() {
    val person1 = Person("test1")
    val person2 = Person("test2")
    val person3 = Person("test3")
    val response = FrogrResponse<Person>()
    response.total = 3
    response.message = "test"
    response.data = mutableListOf(person1, person2)
    response.add(person3)
    response.isSuccess = true
    response.errorCode = 404
    val json = jacksonObjectMapper().writeValueAsString(response)
    val fromJson = jacksonObjectMapper().readValue<FrogrResponse<Person>>(json)
    assertThat(response.errorCode!!).isEqualTo(fromJson.errorCode)
    assertThat(response.total!!).isEqualTo(fromJson.total)
    assertThat(response.isSuccess).isEqualTo(fromJson.isSuccess)
    assertThat(response.message).isEqualTo(fromJson.message)
    assertThat(response.data).isEqualTo(fromJson.data)
    assertThat(response.singleton()).isEqualTo(person1)
  }
}