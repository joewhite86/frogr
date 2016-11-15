package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.SearchParameter;
import org.neo4j.graphdb.index.Index;

import java.util.Map;
import java.util.stream.Stream;

public interface RelationshipRepository<T extends Relationship> extends Repository<T> {
  Stream<T> findIndexed(Index<org.neo4j.graphdb.Relationship> index, String field, Object value);

  Stream<T> findIndexed(Index<org.neo4j.graphdb.Relationship> index, String field, Object value, SearchParameter params);

  T findIndexedSingle(Index<org.neo4j.graphdb.Relationship> index, String field, Object value);

  T findIndexedSingle(Index<org.neo4j.graphdb.Relationship> index, String field, Object value, SearchParameter params);

  org.neo4j.graphdb.Relationship getRelationship(Relationship model);

  Index<org.neo4j.graphdb.Relationship> index();

  Index<org.neo4j.graphdb.Relationship> index(String indexName);

  void index(T model, String name, Object value);

  void index(Index<org.neo4j.graphdb.Relationship> index, T model, String name, Object value);

  Map<String, String> indexConfig(String index);

  Index<org.neo4j.graphdb.Relationship> indexForField(String fieldName);

  void indexRemove(Index<org.neo4j.graphdb.Relationship> index, org.neo4j.graphdb.Relationship node);

  void indexRemove(org.neo4j.graphdb.Relationship node, String field);

  void indexRemove(org.neo4j.graphdb.Relationship node);

  void indexRemove(Index<org.neo4j.graphdb.Relationship> index, org.neo4j.graphdb.Relationship node, String field);
}
