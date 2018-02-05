package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.model.relationship.BaseRelationship;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRelationshipRepository<T extends BaseRelationship> extends BaseRelationshipRepository<T> {
  public DefaultRelationshipRepository(String modelName) {
    super(modelName);
  }

  @Override
  public Class<?> getModelClass() {
    if(modelClass == null) {
      modelClass = service().persistence().cache().getModel(getType());
      if(modelClass == null) modelClass = BaseRelationship.class;
    }

    return modelClass;
  }
}
