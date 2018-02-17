package de.whitefrog.frogr.model

import com.fasterxml.jackson.annotation.JsonView
import de.whitefrog.frogr.rest.Views

abstract class Entity : FBaseImpl(), Model {
  companion object {
    const val Model = "model"
  }
  /**
   * Model to use. Can be used for abstract parent classes to further determine
   * the correct type.
   */
  @JsonView(Views.Hidden::class)
  var model: String? = null

  override fun toString(): String {
    val typeName = if (type() != null) type() else javaClass.simpleName
    val id = if (id == -1L) uuid else id.toString()
    return "$typeName ($id)"
  }
}