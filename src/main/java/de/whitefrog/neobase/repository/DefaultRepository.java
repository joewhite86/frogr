package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;

public class DefaultRepository<T extends Model> extends BaseRepository<T> {
  public DefaultRepository(Service service, String modelName) {
    super(service, modelName);
  }

  @Override
  public void remove(T model) throws PersistException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeRelationship(T model, String field, Model other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void save(T model) throws PersistException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void save(SaveContext<T> context) throws PersistException {
    throw new UnsupportedOperationException();
  }
}
