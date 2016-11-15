package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.rest.SearchParameter;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface ModelRepository<T extends Model> extends Repository<T> {

  Stream<T> findIndexed(Index<Node> index, String field, Object value);

  Stream<T> findIndexed(Index<Node> index, String field, Object value, SearchParameter params);

  T findIndexedSingle(Index<Node> index, String field, Object value);

  T findIndexedSingle(Index<Node> index, String field, Object value, SearchParameter params);

  Node getNode(Model model);

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
}
