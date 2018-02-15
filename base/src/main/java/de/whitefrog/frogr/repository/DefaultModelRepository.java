package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.model.Model;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultModelRepository<T extends Model> extends BaseModelRepository<T> {
  public DefaultModelRepository(String modelName) {
    super(modelName);
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = cache().getModel(getType());
      if(modelClass == null) modelClass = Model.class;
    }

    return modelClass;
  }
}
