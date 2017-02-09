package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.Relationship;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRelationshipRepository<T extends Relationship> extends BaseRelationshipRepository<T> {
  public DefaultRelationshipRepository(Service service, String modelName) {
    super(service, modelName);
  }
}
