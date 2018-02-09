package de.whitefrog.frogr.rest.request

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.whitefrog.frogr.model.FieldList
import de.whitefrog.frogr.model.Filter
import de.whitefrog.frogr.model.SearchParameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.neo4j.helpers.collection.MapUtil
import org.springframework.mock.web.MockHttpServletRequest
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.container.ResourceContext

class TestSearchParameterResolver {
  @Test
  fun parseHeaderJson() {
    val context = mock(ResourceContext::class.java)
    val httpRequest = MockHttpServletRequest()
    val input = SearchParameter()
    input.fields(FieldList.parseFields("friend.{name,age},friend.friend,name,age"))
    input.filter("name", "Rick Sanchez")
    input.filter(Filter.GreaterThan("age", 10L, true))
    input.filter(Filter.LessThan("age", 500L, true))
    input.filter(Filter.NotEquals("name", "*Smith"))
    input.page(2)
    input.count(true)
    httpRequest.addHeader("params", jacksonObjectMapper().writeValueAsString(input))
    `when`(context.getResource(HttpServletRequest::class.java)).thenReturn(httpRequest)

    val factory = SearchParameterResolver.SearchParameterValueFactory()
    factory.setContext(context)
    val params = factory.provide()

    assertThat(params.fieldList()).isNotEmpty
    assertThat(params.fieldList()["age"]).isNotNull()
    assertThat(params.fieldList()["name"]).isNotNull()
    assertThat(params.fieldList()["friend"]!!.subFields()).hasSize(3)

    assertThat(params.filters()).hasSize(4)

    assertThat(params.getFilter("name")[0]).isInstanceOf(Filter.Equals::class.java)
    assertThat(params.getFilter("name")[0].value).isEqualTo("Rick Sanchez")

    assertThat(params.getFilter("name")[1]).isInstanceOf(Filter.NotEquals::class.java)
    assertThat(params.getFilter("name")[1].value).isEqualTo("*Smith")

    assertThat(params.getFilter("age")[0]).isInstanceOf(Filter.GreaterThan::class.java)
    assertThat(params.getFilter("age")[0].value).isEqualTo(10L)
    assertThat((params.getFilter("age")[0] as Filter.GreaterThan).isIncluding).isTrue()

    assertThat(params.getFilter("age")[1]).isInstanceOf(Filter.LessThan::class.java)
    assertThat(params.getFilter("age")[1].value).isEqualTo(500L)
    assertThat((params.getFilter("age")[1] as Filter.LessThan).isIncluding).isTrue()

    assertThat(params.count()).isTrue()
  }
  @Test
  fun parseString() {
    val context = mock(ResourceContext::class.java)
    val httpRequest = MockHttpServletRequest()
    httpRequest.addParameters(MapUtil.stringMap(
      "fields", "friend.{name,age},friend.friend,name,age",
      "filter", "name:=Rick Sanchez,age:>=10,age:<=500,name:!*Smith",
      "count", "true"
    ))
    `when`(context.getResource(HttpServletRequest::class.java)).thenReturn(httpRequest)

    val factory = SearchParameterResolver.SearchParameterValueFactory()
    factory.setContext(context)
    val params = factory.provide()
    
    assertThat(params.fieldList()).isNotEmpty
    assertThat(params.fieldList()["age"]).isNotNull()
    assertThat(params.fieldList()["name"]).isNotNull()
    assertThat(params.fieldList()["friend"]!!.subFields()).hasSize(3)
    
    assertThat(params.filters()).hasSize(4)
    
    assertThat(params.getFilter("name")[0]).isInstanceOf(Filter.Equals::class.java)
    assertThat(params.getFilter("name")[0].value).isEqualTo("Rick Sanchez")

    assertThat(params.getFilter("name")[1]).isInstanceOf(Filter.NotEquals::class.java)
    assertThat(params.getFilter("name")[1].value).isEqualTo("*Smith")
    
    assertThat(params.getFilter("age")[0]).isInstanceOf(Filter.GreaterThan::class.java)
    assertThat(params.getFilter("age")[0].value).isEqualTo(10L)
    assertThat((params.getFilter("age")[0] as Filter.GreaterThan).isIncluding).isTrue()

    assertThat(params.getFilter("age")[1]).isInstanceOf(Filter.LessThan::class.java)
    assertThat(params.getFilter("age")[1].value).isEqualTo(500L)
    assertThat((params.getFilter("age")[1] as Filter.LessThan).isIncluding).isTrue()
    
    assertThat(params.count()).isTrue()
  }
}
