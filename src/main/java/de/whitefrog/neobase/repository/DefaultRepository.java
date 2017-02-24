package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.persistence.Persistence;

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
