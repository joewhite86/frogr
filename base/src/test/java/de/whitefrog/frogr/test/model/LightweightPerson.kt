package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.BaseModel
import de.whitefrog.frogr.model.annotation.Indexed

class LightweightPerson(@Indexed var field: String? = null): BaseModel()