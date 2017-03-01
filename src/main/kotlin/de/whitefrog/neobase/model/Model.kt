package de.whitefrog.neobase.model

interface Model : Base {

    var model: String?

    companion object {
        val Model = "model"
    }
}
