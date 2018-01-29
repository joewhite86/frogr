package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.persistence.Persistence;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRepository<T extends Model> extends BaseModelRepository<T> {
  public DefaultRepository(Service service, String modelName) {
    super(service, modelName);
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = Persistence.cache().getModel(getType());
      if(modelClass == null) modelClass = Model.class;
    }

    return modelClass;
  }
}
