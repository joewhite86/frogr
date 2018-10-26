package de.whitefrog.frogr.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class TestFilters {
  @Test
  fun parseJson() {
    val list = jacksonObjectMapper().readValue<List<Filter>>(File(javaClass.classLoader.getResource("filters.json").file))
    assertThat(list).isNotEmpty
    assertThat(list[0]).isInstanceOf(Filter.Equals::class.java)
    assertThat(list[1]).isInstanceOf(Filter.GreaterThan::class.java)
    assertThat(list[1].value).isEqualTo(10L)
    assertThat(list[2]).isInstanceOf(Filter.Range::class.java)
    assertThat((list[2] as Filter.Range).from).isEqualTo(10L)
    assertThat((list[2] as Filter.Range).to).isEqualTo(20L)
  }
  
  @Test
  fun createJson() {
    val filter = Filter.Equals("test", "test")
    val json = jacksonObjectMapper().writeValueAsString(filter)
    assertThat(json).contains("\"type\":\"eq\"")
  }
}
