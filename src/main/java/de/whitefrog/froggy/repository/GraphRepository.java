package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.model.Graph;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

public class GraphRepository {
  private Service service;
  private Graph graph = null;

  public GraphRepository(Service service) {
    this.service = service;
  }

  public Graph create() {
    if(getGraph() != null) return graph;
    return new Graph();
  }

  public Graph getGraph() {
    if(graph != null) return graph;
    Result results = service.graph().execute("match (n:Graph) return n");
    if(results.hasNext()) {
      Node node = (Node) results.next().get("n");
      graph = new Graph();
      if(node.hasProperty(Graph.Companion.getVersion())) {
        graph.setVersion((String) node.getProperty(Graph.Companion.getVersion()));
      }
    }
    return graph;
  }
  
  public void save(Graph graph) {
    if(this.graph != null) {
      Result results = service.graph().execute("match (n:Graph) return n");
      Node node = (Node) results.next().get("n");
      node.setProperty(Graph.Companion.getVersion(), graph.getVersion());
    } else {
      Node node = service.graph().createNode(Label.label("Graph"));
      node.setProperty(Graph.Companion.getVersion(), graph.getVersion());
      this.graph = graph;
    }
  }
}
