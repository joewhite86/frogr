package de.whitefrog.frogr.cypher

/**
 * Includes a query as string and its parameters as stringmap.
 * Used in the QueryBuilder.
 */
class Query(private var query: String?, private var params: Map<String, Any>?) {

  fun query(): String? {
    return query
  }

  fun query(query: String) {
    this.query = query
  }

  fun params(): Map<String, Any>? {
    return params
  }

  fun params(params: Map<String, Any>) {
    this.params = params
  }
}
