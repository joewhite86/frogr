package de.whitefrog.frogr.test.rest

import de.whitefrog.frogr.rest.service.DefaultCRUDService
import de.whitefrog.frogr.test.model.Clothing
import javax.ws.rs.Path

@Path("clothing")
class Clothings: DefaultCRUDService<Clothing>()