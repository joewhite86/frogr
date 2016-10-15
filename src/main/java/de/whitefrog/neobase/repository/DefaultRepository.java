package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.*;
import de.whitefrog.neobase.collection.ListIterator;
import de.whitefrog.neobase.cypher.BaseQueryBuilder;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.exception.NeobaseRuntimeException;
import de.whitefrog.neobase.exception.PersistException;
import de.whitefrog.neobase.exception.RepositoryInstantiationException;
import de.whitefrog.neobase.exception.TypeMismatchException;
import de.whitefrog.neobase.helper.ReflectionUtil;
import de.whitefrog.neobase.index.IndexUtils;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.annotation.RelatedTo;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

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
