package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.relationship.Relationship;

public interface RelationshipRepository<T extends Relationship> extends Repository<T> {
  org.neo4j.graphdb.Relationship getRelationship(Relationship model);
}
