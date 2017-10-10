package de.whitefrog.neobase.cypher

/**
 * Builds the MATCH part of a neo4j cypher query.
 * Brings labels, relationship type names and variables in the correct form.
 */
class MatchBuilder {
  private var from: String = ""
  private var fromLabel: String = ""
  private var relationship: String = ""
  private var relationshipType: String = ""
  private var to: String = ""
  private var toLabel: String = ""

  fun from(from: String): MatchBuilder {
    this.from = from
    return this
  }

  fun fromLabel(label: String): MatchBuilder {
    this.fromLabel = ":$label"
    return this
  }

  fun relationship(relationship: String): MatchBuilder {
    this.relationship = relationship
    return this
  }

  fun relationshipType(type: String): MatchBuilder {
    this.relationshipType = ":$type"
    return this
  }

  fun to(to: String): MatchBuilder {
    this.to = to
    return this
  }
  
  fun toLabel(toLabel: String): MatchBuilder {
    this.toLabel = ":$toLabel"
    return this
  }

  fun build(): String {
    return "($from$fromLabel)-[$relationship$relationshipType]->($to$toLabel)"
  }
}
