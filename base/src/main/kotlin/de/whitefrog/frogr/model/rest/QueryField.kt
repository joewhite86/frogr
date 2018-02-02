package de.whitefrog.frogr.model.rest

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import java.util.*

/**
 * Single query field used in rest queries. Can have sub-fields and limit and skip values.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
data class QueryField @JvmOverloads constructor(var field: String, val addAll: Boolean = false) {
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var skip = 0
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var limit = SearchParameter.DefaultLimit
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private var subFields = FieldList()

  init {
//    if (field.contains(".")) {
//      val fields = field.split("\\.".toRegex(), 2).toTypedArray()
//      parseField(fields[0])
//      this.subFields = FieldList.parseFields(Arrays.asList(fields[1]), addAll)
//    } else {
      parseField(field)
//    }
  }

  private fun parseField(field: String) {
    if (field.contains("(")) {
      this.field = field.substring(0, field.indexOf("("))
      var limit = field.substring(field.indexOf("(") + 1, field.length - 1)
      if (limit.contains(";")) {
        this.skip = Integer.parseInt(limit.substring(0, limit.indexOf(";")))
        limit = limit.substring(limit.indexOf(";") + 1)
        this.limit = if (limit == "max") Integer.MAX_VALUE else Integer.parseInt(limit)
      } else {
        this.limit = if (limit == "max") Integer.MAX_VALUE else Integer.parseInt(limit)
      }
    } else {
      this.field = field
    }
  }

  fun field(): String {
    return field
  }

  fun limit(): Int {
    return limit
  }

  fun limit(limit: Int) {
    this.limit = limit
  }

  fun skip(): Int {
    return skip
  }

  fun skip(skip: Int) {
    this.skip = skip
  }

  fun subFields(): FieldList {
    return subFields
  }

  fun subFields(fields: FieldList) {
    this.subFields = fields
  }

  fun subFields(vararg fd: QueryField) {
    this.subFields.addAll(Arrays.asList(*fd))
  }

  override fun hashCode(): Int {
    return HashCodeBuilder()
      .append(field)
      .toHashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (other !is QueryField) return false
    return EqualsBuilder()
      .append(field, other.field)
      .isEquals
  }

  override fun toString(): String {
    var str = field
    if(!subFields().isEmpty()) {
      str += "{"
      subFields().forEachIndexed { i, f ->
        str += f.toString() +
          (if (f.limit != SearchParameter.DefaultLimit) "(limit: " + f.limit + ")" else "") +
          (if (i + 1 < subFields().size) "," else "")
      }
      str += "}"
    }
    return str
  }
}
