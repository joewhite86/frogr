package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.Model;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Set;

public interface ModelRepository<T extends Model> extends Repository<T> {
  Node getNode(Model model);
  /**
   * Get the main label
   *
   * @return Main label
   */
  Label label();
  Set<Label> labels();
}
