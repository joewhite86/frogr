package de.whitefrog.frogr.model

interface FBase: Base {
  var uuid: String?
  var lastModified: Long?
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