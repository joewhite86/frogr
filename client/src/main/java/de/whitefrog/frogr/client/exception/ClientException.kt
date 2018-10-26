package de.whitefrog.frogr.client.exception

import de.whitefrog.frogr.exception.FrogrException

open class ClientException(message: String?, cause: Exception? = null): FrogrException(message, cause)