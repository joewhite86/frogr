package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.collection.ResultIterator;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Repository<T extends de.whitefrog.neobase.model.Model> {
  /**
   * Returns the overall count.
   * @return how many entities of this type exists
   */
  long count();

  /**
   * Execute a filtered query and return the overall count of all found entities.
   * @param params search parameters
   * @return how many entities were found
   */
  long count(SearchParameter params);

  boolean contains(T entity);

  T createModel(Node node);

  T createModel(Node node, FieldList fields);

  /**
   * Get a node by id
   *
   * @param id Node id
   * @return The node if found, otherwise a Exception will be thrown
   */
  T find(long id, String... fields);

  T find(long id, List<String> fields);

  ResultIterator<T> find(String property, Object value);

  ResultIterator<T> find(SearchParameter params);

  ResultIterator<T> findAll();

  T findByUuid(String uuid);

  T findSingle(String property, Object value);

  T fetch(T tag);

  T fetch(T tag, String... fields);

  T fetch(T tag, boolean refetch, String... fields);

  T fetch(T tag, FieldList fields);

  T fetch(T tag, boolean refetch, FieldList fields);

  /**
   * Get a list of all nodes of this type
   *
   * @return List of all nodes
   */
  ResultIterator<T> findAll(int limit, int page);

  /**
   * Get a list of entities, which changed since a particular date
   *
   * @param timestamp Timestamp after which nodes should be returned
   * @param limit     Limit the amount of nodes returned
   * @return List of nodes, which changed after timestamp
   */
  ResultIterator<T> findChangedSince(long timestamp, int limit, int page);

  ResultIterator<T> findIndexed(String field, Object value);

  ResultIterator<T> findIndexed(String field, Object value, SearchParameter params);

  ResultIterator<T> findIndexed(Index<Node> index, String field, Object value);

  ResultIterator<T> findIndexed(Index<Node> index, String field, Object value, SearchParameter params);

  T findIndexedSingle(String field, Object value);

  T findIndexedSingle(String field, Object value, SearchParameter params);

  T findIndexedSingle(Index<Node> index, String field, Object value);

  T findIndexedSingle(Index<Node> index, String field, Object value, SearchParameter params);

  void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship);

  void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship, String... fields);

  void fetch(de.whitefrog.neobase.model.relationship.Relationship relationship, FieldList fields);

  boolean filter(Node node, Collection<SearchParameter.PropertyFilter> filters);

  Class<?> getModelClass();

  Node getNode(Model model);
  
  /**
   * Get the database instance associated with the controller
   *
   * @return Database instance
   */
  GraphDatabaseService graph();

  Index<Node> index();

  Index<Node> index(String indexName);

  void index(T model, String name, Object value);

  void index(Index<Node> index, T model, String name, Object value);

  Map<String, String> indexConfig(String index);

  Index<Node> indexForField(String fieldName);

  void indexRemove(Index<Node> index, Node node);

  void indexRemove(Node node, String field);

  void indexRemove(Node node);

  void indexRemove(Index<Node> index, Node node, String field);

  /**
   * Get the main label
   *
   * @return Main label
   */
  Label label();

  Set<Label> labels();

  ResultIterator<T> query(String query);

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

  ResultIterator<T> search(String query);

  ResultIterator<T> search(SearchParameter params);

  <R extends Base> ResultIterator<R> searchRelated(SearchParameter params);

  Service service();

  void sort(List<T> list, List<SearchParameter.OrderBy> orderBy);

  void dispose();
}
