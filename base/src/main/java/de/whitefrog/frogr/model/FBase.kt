package de.whitefrog.frogr.model

interface FBase: Base {
  var uuid: String?
  var lastModified: Long?
  var created: Long?
  fun updateLastModified()
  
  companion object {
    const val LastModified = "lastModified"
    const val Created = "created"
    const val Uuid = "uuid"
  }
}