package de.whitefrog.frogr.model

import de.whitefrog.frogr.exception.FrogrException
import de.whitefrog.frogr.exception.PersistException
import de.whitefrog.frogr.persistence.FieldDescriptor
import de.whitefrog.frogr.repository.Repository
import org.neo4j.graphdb.PropertyContainer
import java.lang.reflect.Field

/**
 * Context for entity/relationship save operations.
 * Takes one entity or relationship and tests for property and relationship changes.
 */
class SaveContext<T : Base>(
  /**
   * The repository used in the save context.
   */
  private val repository: Repository<T>,
  /**
   * Model used for this save operation.
   */
  private val model: T) {
  /**
   * The original fetched from database.
   */
  private var original: T? = null
  /**
   * The neo4j node or relationship.
   */
  private var node: PropertyContainer? = null
  /**
   * List of FieldDescriptor's containing fields that
   * changed compared to the original.
   */
  private var changedFields: List<FieldDescriptor<*>>? = null
  /**
   * Reference to the model's field map.
   */
  private val fieldMap: List<FieldDescriptor<*>>

  init {
    if (model.id > -1) {
      original = repository.createModel(node())
    } else if (model is FBase && model.uuid != null) {
      original = repository.findByUuid(model.uuid)
      model.id = original!!.id
    }
    fieldMap = repository.cache().fieldMap(model.javaClass)
  }

  /**
   * Get a full list of changed fields.
   * @return list of changed fields
   */
  fun changedFields(): List<FieldDescriptor<*>> {
    if (changedFields == null) {
      if (original() != null) repository.fetch(original(), Base.AllFields)
      changedFields = fieldMap
        .filter { f -> fieldChanged(f.field()) }
    }
    return changedFields!!
  }

  /**
   * Test if a single field has changed.
   * @param fieldName field name
   * @return <code>true</code> if the field has changed, otherwise <code>false</code>
   */
  fun fieldChanged(fieldName: String): Boolean {
    return changedFields().stream().anyMatch { f -> f.name == fieldName }
  }

  private fun fieldChanged(field: Field): Boolean {
    val annotation = repository.cache().fieldAnnotations(repository().modelClass, field.name)
    try {
      if (!field.isAccessible) field.isAccessible = true
      val value = field.get(model)
      if (value != null && !annotation!!.nullRemove) {
        if (original() == null) {
          return true
        } else {
          if (annotation.relatedTo != null && annotation.lazy) return true
          if (annotation.relatedTo != null) repository().fetch(original(), FieldList.parseFields(field.name + "(max)"))
          val originalValue = field.get(original())
          return value != originalValue
        }
      } else if (annotation!!.nullRemove) {
        return true
      }
    } catch (e: IllegalAccessException) {
      throw FrogrException(e.message, e)
    }

    return false
  }

  /**
   * Get the field map for the model in this context.
   * @return field map of the model in this context
   */
  fun fieldMap(): List<FieldDescriptor<*>> {
    return fieldMap
  }

  /**
   * Get the current model.
   * @return the model in this context
   */
  fun model(): T {
    return model
  }

  /**
   * Get the model repository.
   * @return the model repository
   */
  fun repository(): Repository<T> {
    return repository
  }

    /**
   * Get the neo4j node or relationship used as reference in this context.
   * @return the neo4j node or relationship used as reference in this context
   */ 
  @Suppress("UNCHECKED_CAST")
  fun <N : PropertyContainer> node(): N {
    if (node == null) {
      node = if (original() != null) {
        if (model is Model)
          repository.graph().getNodeById(original()!!.id)
        else
          repository.graph().getRelationshipById(original()!!.id)
      } else if (model().id > -1) {
        if (model is Model)
          repository.graph().getNodeById(model().id)
        else
          repository.graph().getRelationshipById(model().id)
      } else {
        throw PersistException("could not resolve model $model")
      }
    }
    return node as N
  }

  /**
   * The original model used as reference in this context.
   * @return the original model used as reference in this context
   */
  fun original(): T? {
    return original
  }

  /**
   * Set the neo4j node. Used when a new node is created.
   * !!! Should not be used outside of persistence methods.
   * @param node the neo4j node
   */
  fun setNode(node: PropertyContainer) {
    this.node = node
  }
}
