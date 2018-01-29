package de.whitefrog.frogr.model.rest

import com.fasterxml.jackson.annotation.JsonAutoDetect
import de.whitefrog.frogr.model.Entity

import java.util.Arrays
import java.util.HashSet

/**
 * Field list used in rest queries. Contains multiple QueryField instances.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
class FieldList : HashSet<QueryField>() {

  fun containsField(name: String): Boolean {
    return this.any { it.field() == name }
  }

  operator fun get(name: String): QueryField? {
    return this.firstOrNull { it.field() == name }
  }

  companion object {
    @JvmField
    var All = FieldList.parseFields(Entity.AllFields)

    @JvmStatic
    fun create(vararg fields: QueryField): FieldList {
      val list = FieldList()
      list.addAll(Arrays.asList(*fields))
      return list
    }

    @JvmStatic
    fun parseFields(vararg fields: String): FieldList {
      return parseFields(Arrays.asList(*fields), false)
    }

    @JvmStatic @JvmOverloads 
    fun parseFields(fields: List<String>, addAll: Boolean = false): FieldList {
      val fieldList = FieldList()

      for (field in fields) {
        if (field.contains(".")) {
          val fieldName = field.substring(0, field.indexOf("."))
          if (fieldList.containsField(fieldName)) {
            fieldList[fieldName]!!.subFields(QueryField(field.substring(field.indexOf(".") + 1), addAll))
            continue
          }
        } else if (field.startsWith("[")) {
          // assuming sth like user.[name;login]
          return parseFields(*field.substring(1, field.length - 1)
            .split(";".toRegex()).dropLastWhile(String::isEmpty).toTypedArray())
        }
        val queryField = QueryField(field, addAll)
        if (addAll) queryField.subFields(QueryField(Entity.AllFields))
        fieldList.add(queryField)
      }

      if (addAll) fieldList.add(QueryField(Entity.AllFields))
      return fieldList
    }
  }

}
