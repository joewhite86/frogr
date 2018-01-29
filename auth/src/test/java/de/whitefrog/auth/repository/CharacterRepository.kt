package de.whitefrog.auth.repository

import de.whitefrog.auth.model.Character
import de.whitefrog.frogr.Service
import de.whitefrog.frogr.repository.BaseModelRepository

class CharacterRepository(service: Service) : BaseModelRepository<Character>(service)
