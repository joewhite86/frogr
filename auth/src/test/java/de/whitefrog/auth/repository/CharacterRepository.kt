package de.whitefrog.auth.repository

import de.whitefrog.auth.model.Character
import de.whitefrog.froggy.Service
import de.whitefrog.froggy.repository.BaseModelRepository

class CharacterRepository(service: Service) : BaseModelRepository<Character>(service)
