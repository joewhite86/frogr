package de.whitefrog.neobase.model.relationship

import de.whitefrog.neobase.model.Base

interface Relationship<From, To> : Base {
    val from: From
    val to: To
}
