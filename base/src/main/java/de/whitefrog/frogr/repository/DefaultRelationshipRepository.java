package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.model.relationship.BaseRelationship;
import de.whitefrog.frogr.persistence.Persistence;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRelationshipRepository<T extends BaseRelationship> extends BaseRelationshipRepository<T> {
  public DefaultRelationshipRepository(Service service, String modelName) {
    super(service, modelName);
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = Persistence.cache().getModel(getType());
      if(modelClass == null) modelClass = BaseRelationship.class;
    }

    return modelClass;
  }
}
