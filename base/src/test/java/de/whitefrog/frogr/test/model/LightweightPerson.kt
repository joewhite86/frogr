package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.BaseImpl
import de.whitefrog.frogr.model.Model
import de.whitefrog.frogr.model.annotation.Indexed

class LightweightPerson(@Indexed var field: String? = null): BaseImpl(), Model