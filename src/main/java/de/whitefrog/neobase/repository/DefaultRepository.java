package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;

/**
 * Will be used by {@link RepositoryFactory} method when no other repository was found. 
 */
public class DefaultRepository<T extends Model> extends BaseRepository<T> {
  public DefaultRepository(Service service, String modelName) {
    super(service, modelName);
  }
}
