package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.model.Model;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Set;

public interface ModelRepository<T extends Model> extends Repository<T> {
  T createModel();

  Node getNode(Model model);
  /**
   * Get the main label
   *
   * @return Main label
   */
  Label label();
  Set<Label> labels();
}
