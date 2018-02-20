package de.whitefrog.frogr.exception

class ModelValidationException(message: String, val field: String) : PersistException(message)
