package de.whitefrog.examples.simpsons.repository;

import de.whitefrog.examples.simpsons.model.Character;
import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.repository.BaseModelRepository;

public class CharacterRepository extends BaseModelRepository<Character> {
  public CharacterRepository(Service service) {
    super(service);
  }
}
