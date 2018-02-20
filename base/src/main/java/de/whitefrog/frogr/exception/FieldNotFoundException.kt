package de.whitefrog.frogr.exception

import de.whitefrog.frogr.model.Base

class FieldNotFoundException(field: String, model: Base) : PersistException(field + " on " + model.javaClass.simpleName)
