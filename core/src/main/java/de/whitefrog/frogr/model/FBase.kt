package de.whitefrog.frogr.model

interface FBase: Base {
  /**
   * Unique identifier. String based, guaranteed unique identifier.
   */
  var uuid: String?

  /**
   * Timestamp, updated each time the entity is perstisted.
   */
  var lastModified: Long?

  /**
   * Timestamp, automatically set on first persist.
   */
  var created: Long?

  /**
   * Update the last modified timestamp to current.
   */
  fun updateLastModified() {
    this.lastModified = System.currentTimeMillis()
  }
  
  companion object {
    const val LastModified = "lastModified"
    const val Created = "created"
    const val Uuid = "uuid"
  }
}