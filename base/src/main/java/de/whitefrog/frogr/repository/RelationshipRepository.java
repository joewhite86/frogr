package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.relationship.Relationship;

/**
 * Repository for relationships. 
 */
public interface RelationshipRepository<T extends Relationship> extends Repository<T> {
  /**
   * Creates a relationship model between two models.
   * @param from Relationship starts here
   * @param to Relationship ends here
   * @return The created relationship model instance
   */
  T createModel(Model from, Model to);

  /**
   * Get the underlying neo4j relationship for a relationship model.
   * @param model Relationship model used to get the neo4j relationship
   * @return Neo4j relationship for the passed relationship model
   */
  org.neo4j.graphdb.Relationship getRelationship(Relationship model);
}
