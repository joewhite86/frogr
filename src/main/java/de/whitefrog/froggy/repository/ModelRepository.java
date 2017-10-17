package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.model.Model;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Set;

/**
 * Repository for default models.
 */
public interface ModelRepository<T extends Model> extends Repository<T> {
  /**
   * Create a new/empty model instance.
   * @return The created model instance
   */
  T createModel();

  /**
   * Get the underlying neo4j node for a model.
   * @param model Model which represents the neo4j node
   * @return The underlying neo4j node for the passed model
   */
  Node getNode(Model model);
  /**
   * Get the main label to use. Most commonly this is the same as the model class name.
   * @return Main label
   */
  Label label();

  /**
   * Set of labels used for models of this repository.
   * @return Set of labels used for models of this repository
   */
  Set<Label> labels();
}
