package de.whitefrog.frogr.model

import de.whitefrog.frogr.exception.FrogrException
import junit.framework.Assert.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestFieldList {
  @Test
  fun parseFields() {
    val input = "name,marriedWith.{to.name,from.{name,age},years(100)},children.{age,name}, children.{children}"
    val fields = FieldList.parseFields(input)
    assertThat(fields).hasSize(3)
    assertThat(fields.containsField("name")).isTrue()
    assertThat(fields.containsField("marriedWith")).isTrue()
    assertThat(fields.containsField("children")).isTrue()
    assertThat(fields["marriedWith"]!!.subFields()).hasSize(3)
    assertThat(fields["marriedWith"]!!.subFields()["to"]!!.subFields()).hasSize(1)
    assertEquals(100, fields["marriedWith"]!!.subFields()["years"]!!.limit())
  }
  @Test
  fun parseFieldsAsArray() {
    val list = mutableListOf("name", "marriedWith.{to.name,from.{name,age},years(100)}", "children.{age,name}", "children.{children}")
    val fields = FieldList.parseFields(list)
    assertThat(fields).hasSize(3)
    assertThat(fields.containsField("name")).isTrue()
    assertThat(fields.containsField("marriedWith")).isTrue()
    assertThat(fields.containsField("children")).isTrue()
    assertThat(fields["marriedWith"]!!.subFields()).hasSize(3)
    assertEquals(100, fields["marriedWith"]!!.subFields()["years"]!!.limit())
  }
  @Test
  fun subFieldsAndLimit() {
    val input = "name,friends(33).{name,age},age"
    val fields = FieldList.parseFields(input)
    assertThat(fields).hasSize(3)
    assertThat(fields.containsField("name"))
    assertThat(fields.containsField("friends"))
    assertThat(fields.containsField("age"))
    assertThat(fields["friends"]!!.subFields()).hasSize(2)
    assertEquals(33, fields["friends"]!!.limit())
  }
  @Test(expected = FrogrException::class)
  fun wrongSubfieldFormat() {
    val list = mutableListOf("name", "marriedWith.[to.name,from.{name,age},years(100)]", "children.[age,name]", "children.[children]")
    FieldList.parseFields(list)
  }
}
