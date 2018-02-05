package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.model.Model;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRepository<T extends Model> extends BaseModelRepository<T> {
  public DefaultRepository(String modelName) {
    super(modelName);
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = service().persistence().cache().getModel(getType());
      if(modelClass == null) modelClass = Model.class;
    }

    return modelClass;
  }
}
