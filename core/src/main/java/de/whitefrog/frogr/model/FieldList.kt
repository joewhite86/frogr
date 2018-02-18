package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.exception.QueryParseException
import java.util.*

/**
 * Field list used in rest queries. Contains multiple QueryField instances.
 */
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.ANY, 
  getterVisibility = JsonAutoDetect.Visibility.NONE, 
  setterVisibility = JsonAutoDetect.Visibility.NONE
)
class FieldList() : HashSet<QueryField>() {
  constructor(list: FieldList): this() {
    addAll(HashSet(list))
  }
  fun containsField(name: String): Boolean {
    return this.any { it.field == name }
  }

  operator fun get(name: String): QueryField? {
    return this.firstOrNull { it.field == name }
  }
  
  fun getOrEmpty(name: String): QueryField {
    return firstOrNull { it.field == name } ?: QueryField(name)
  }

  override fun toString(): String {
    var out = ""
    this.forEachIndexed { i, field ->
      out+= field
      if(i + 1 < size) out+= ","
    }
    return out
  }

  companion object {
    @JvmField
    var All = parseFields(Base.AllFields)

    @JvmStatic
    fun parseFields(vararg fields: String): FieldList {
      return parseFields(Arrays.asList(*fields))
    }
    
    @JvmStatic
    fun parseFields(stringFields: List<String>) : FieldList {
      val fields = FieldList()

      stringFields
        .map { if(it.contains(".")) parseField(it, fields) else QueryField(it) }
        .forEach { 
          if(fields.containsField(it.field)) {
            fields[it.field]!!.subFields().addAll(it.subFields())
          } else {
            fields.add(it)
          } 
        }
      
      return fields
    }
    
    @JvmStatic
    fun parseFields(input: String) : FieldList {
      val fields = FieldList()

      var field = ""
      var brackets = 0
      var inLimitString = false
      for(char in input) {
        when {
          char == '}' -> {
            if(brackets == 0) throw QueryParseException("missing {")
            brackets--
            field+= char
          }
          char == '{' -> {
            brackets++
            field+= char
          }
          brackets > 0 || char != ',' -> {
            if(char == '(') inLimitString = true
            else if(char == ')') inLimitString = false
            else if(brackets == 0 && !inLimitString && !char.isLetter() && char != '.' && char != ' ') 
              throw FrogrException("cannot parse character '$char' ($input)")
            
            if(char != ' ') field+= char
          }
          else -> {
            // brackets is 0 and current char is ',' (end of field)
            fields.add(parseField(field, fields))
            field = ""
          }
        }
      }
      if(brackets > 0) {
        throw QueryParseException("missing }")
      }
      if(!field.isEmpty()) {
        fields.add(parseField(field, fields))
      }
      
      return fields
    }
    
    @JvmStatic
    private fun parseField(pFieldString: String, parentFields: FieldList) : QueryField {
      val queryField: QueryField?
      val fieldString = pFieldString.trim()

      // if there's a "." in the string, we have to parse the subfields too
      if(fieldString.contains(".")) {
        val field = fieldString.substring(0, fieldString.indexOf("."))
        var subFieldString = fieldString.substring(field.length + 1, fieldString.length) 
        
        if(subFieldString.startsWith("{")) {
          if(!subFieldString.endsWith("}")) throw QueryParseException("missing }")
          subFieldString = subFieldString.substring(1, subFieldString.length - 1)
        }
        // if we already have the field in the parent FieldList, we can add all subfields there
        if(parentFields.containsField(field)) {
          queryField = parentFields[field]!!
          queryField.subFields().addAll(parseFields(subFieldString))
        } 
        // ... else we create a new one and add all subfields there
        else {
          queryField = QueryField(field)
          queryField.subFields(parseFields(subFieldString))
        }
      } 
      // ... else we have a plain QueryField
      else {
        queryField = QueryField(fieldString)
      }
      
      return queryField
    }
  }
}
