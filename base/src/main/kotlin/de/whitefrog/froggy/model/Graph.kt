package de.whitefrog.froggy.model

class Graph : Entity() {
    var version: String? = null

    companion object {
        @JvmField val Version = "version"
    }
}
