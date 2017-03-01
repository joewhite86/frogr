package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.service.Search;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface Repository<T extends Base> {
  boolean contains(T entity);

  T createModel(PropertyContainer node);

  T createModel(PropertyContainer node, FieldList fields);

  void dispose();

  T fetch(T model, String... fields);

  T fetch(T model, FieldList fields);

  T fetch(T model, boolean refetch, FieldList fields);

  /**
   * Get a node by id
   *
   * @param id PropertyContainerode id
   * @return The node if found, otherwise a Exception will be thrown
   */
  T find(long id, String... fields);

  T find(long id, List<String> fields);

  T find(long id, FieldList fields);

  T findByUuid(String uuid);

  String getType();

  Class<?> getModelClass();
  
  /**
   * Get the database instance associated with the controller
   *
   * @return Database instance
   */
  GraphDatabaseService graph();

  QueryBuilder queryBuilder();

  String queryIdentifier();

  /**
   * Delete a model
   *
   * @param model Model to delete
   */
  void remove(T model);

  void save(T model);

  void save(T... entities);

  void save(SaveContext<T> context);

  Search search();

  Service service();

  void sort(List<T> list, List<SearchParameter.OrderBy> orderBy);
}
