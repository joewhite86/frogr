package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.model.relationship.Relationship;

public interface RelationshipRepository<T extends Relationship> extends Repository<T> {
  T createModel(Model from, Model to);

  org.neo4j.graphdb.Relationship getRelationship(Relationship model);
}
