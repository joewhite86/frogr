package de.whitefrog.neobase;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRepositories {
  @Test
  public void correctLabel() {
    TestModelRepository repository = new TestModelRepository(TestNeobase.service());
    assertThat(repository.label().name()).isEqualTo("TestModel");
  }
}
