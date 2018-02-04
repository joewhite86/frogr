package de.whitefrog.frogr.model

import de.whitefrog.frogr.model.FieldList
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

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
    assertThat(fields["marriedWith"]!!.subFields()["to"]!!.subFields()).hasSize(1)
  }
}
