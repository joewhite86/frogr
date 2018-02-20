package de.whitefrog.frogr.model

class Graph : BaseModel() {
    var version: String? = null

    companion object {
        const val Version = "version"
    }
}
