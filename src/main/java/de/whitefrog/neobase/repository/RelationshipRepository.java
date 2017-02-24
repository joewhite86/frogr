package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.Relationship;

public interface RelationshipRepository<T extends Relationship> extends Repository<T> {
  T createModel(Model from, Model to);

  org.neo4j.graphdb.Relationship getRelationship(Relationship model);
}
