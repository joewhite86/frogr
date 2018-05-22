package de.whitefrog.frogr.cypher

import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.model.annotation.IndexType
import de.whitefrog.frogr.persistence.FieldDescriptor
import de.whitefrog.frogr.repository.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class FieldParser(val repository: Repository<*>) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(FieldParser::class.java)
  }
  fun parse(value: String): ArrayList<FieldDescriptor<*>> {
    val fields = value.split(".")
    val descriptors = ArrayList<FieldDescriptor<*>>()
    var index = 0
    var clazz = repository.modelClass
    
    while(index < fields.size) {
      val descriptor = repository.cache().fieldDescriptor(clazz, fields[index])
      if(descriptor != null) {
        descriptors.add(descriptor)

        if (Base::class.java.isAssignableFrom(descriptor.baseClass())) {
          clazz = descriptor.baseClass()
        }
      } else {
        throw FrogrException("field ${fields[index]} could not be found on ${clazz.name}")
      }
      
      index++
    }
    
    return descriptors
  }
  
  fun isLowerCase(value: String): Boolean {
    return try {
      val field = parse(value).last()
      field.annotations().indexed?.type == IndexType.LowerCase
    } catch(e: FrogrException) {
      logger.warn(e.message, e)
      false
    }
  }
}