package de.whitefrog.froggy.model

/**
 * Base interface for all model entities.
 */
interface Model : Base {

  var model: String?

  companion object {
    val Model = "model"
  }
}
