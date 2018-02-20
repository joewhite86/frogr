package de.whitefrog.frogr.repository

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.model.Graph
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node

class GraphRepository(private val service: Service) {
  private var graph: Graph? = null

  fun create(): Graph? {
    return if (getGraph() != null) graph else Graph()
  }

  fun getGraph(): Graph? {
    if (graph != null) return graph
    val results = service.graph().execute("match (n:Graph) return n")
    if (results.hasNext()) {
      val node = results.next()["n"] as Node
      graph = Graph()
      if (node.hasProperty(Graph.Version)) {
        graph!!.version = node.getProperty(Graph.Version) as String
      }
    }
    return graph
  }

  fun save(graph: Graph) {
    if (this.graph != null) {
      val results = service.graph().execute("match (n:Graph) return n")
      val node = results.next()["n"] as Node
      if (graph.version != null) node.setProperty(Graph.Version, graph.version)
    } else {
      val node = service.graph().createNode(Label.label("Graph"))
      if (graph.version != null) node.setProperty(Graph.Version, graph.version)
      this.graph = graph
    }
  }
}
