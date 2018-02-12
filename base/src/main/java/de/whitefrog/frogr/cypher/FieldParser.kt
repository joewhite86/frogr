package de.whitefrog.frogr.cypher

import de.whitefrog.frogr.model.Base
import de.whitefrog.frogr.persistence.FieldDescriptor
import de.whitefrog.frogr.repository.Repository
import java.util.*

class FieldParser(val repository: Repository<*>) {
  fun parse(value: String): ArrayList<FieldDescriptor<*>> {
    val fields = value.split(".")
    val descriptors = ArrayList<FieldDescriptor<*>>()
    var index = 0
    var clazz = repository.modelClass
    
    while(index < fields.size) {
      val descriptor = repository.persistence().cache().fieldDescriptor(clazz, fields[index])
      descriptors.add(descriptor)

      if (Base::class.java.isAssignableFrom(descriptor.baseClass())) {
        clazz = descriptor.baseClass()
      }
      
      index++
    }
    
    return descriptors
  }
}