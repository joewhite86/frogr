package de.whitefrog.frogr.model

/**
 * Base interface for all model entities.
 */
interface Model : Base {
  /**
   * Type identifier. Used to determine the model class to use for this entity.
   */
  var type: String?

  /**
   * Returns the entity type.
   */
  fun type(): String? {
    return type
  }
  
  companion object {
    const val Type = "type"
  }
}
