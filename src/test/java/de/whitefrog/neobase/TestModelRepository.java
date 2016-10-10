package de.whitefrog.neobase;

import de.whitefrog.neobase.repository.BaseRepository;

class TestModelRepository extends BaseRepository<TestModel> {
  public TestModelRepository(Service service) {
    super(service);
  }
}
