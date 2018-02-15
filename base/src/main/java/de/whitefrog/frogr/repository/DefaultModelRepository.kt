package de.whitefrog.frogr.repository

import de.whitefrog.frogr.model.Model

/**
 * Will be used by [RepositoryFactory] method when no other repository was found.
 */
class DefaultModelRepository<T : Model>(modelName: String) : BaseModelRepository<T>(modelName) {
  override fun getModelClass(): Class<*> {
    if (modelClass == null) {
      modelClass = cache().getModel(type)
      if (modelClass == null) modelClass = Model::class.java
    }

    return modelClass
  }
}
