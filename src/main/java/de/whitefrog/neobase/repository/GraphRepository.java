package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.model.Graph;
import de.whitefrog.neobase.Service;
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
    graph = new Graph();
    return graph;
  }

  public Graph getGraph() {
    if(graph != null) return graph;
    Result results = service.graph().execute("match (n:Graph) return n");
    if(results.hasNext()) {
      Node node = (Node) results.next().get("n");
      graph = new Graph();
      if(node.hasProperty(Graph.Version)) {
        graph.setVersion((String) node.getProperty(Graph.Version));
      }
    }
    return graph;
  }
  
  public void save(Graph graph) {
    Result results = service.graph().execute("match (n:Graph) return n");
    if(results.hasNext()) {
      Node node = (Node) results.next().get("n");
      node.setProperty(Graph.Version, graph.getVersion());
      this.graph = new Graph();
      this.graph.setVersion((String) node.getProperty(Graph.Version));
    }
  }
}
