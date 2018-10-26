package de.whitefrog.frogr.client.exception

class UnauthorizedException(message: String?, cause: Exception? = null): ClientException(message, cause)