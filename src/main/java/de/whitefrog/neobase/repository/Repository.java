package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface Repository<T extends Base> {
  boolean contains(T entity);

  T createModel(PropertyContainer node);

  T createModel(PropertyContainer node, FieldList fields);

  void dispose();

  T fetch(T tag, String... fields);

  T fetch(T tag, FieldList fields);

  T fetch(T tag, boolean refetch, FieldList fields);

  boolean filter(Node node, Collection<SearchParameter.PropertyFilter> filters);

  /**
   * Get a node by id
   *
   * @param id Node id
   * @return The node if found, otherwise a Exception will be thrown
   */
  T find(long id, String... fields);

  T find(long id, List<String> fields);

  Stream<T> find(String property, Object value);

  Stream<T> find(SearchParameter params);

  T findByUuid(String uuid);

  T findSingle(String property, Object value);

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

  Stream<T> findIndexed(String field, Object value);

  Stream<T> findIndexed(String field, Object value, SearchParameter params);

  T findIndexedSingle(String field, Object value);

  T findIndexedSingle(String field, Object value, SearchParameter params);
}
