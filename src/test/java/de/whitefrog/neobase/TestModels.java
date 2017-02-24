package de.whitefrog.neobase;

import de.whitefrog.neobase.repository.RelationshipRepository;
import de.whitefrog.neobase.test.Likes;
import de.whitefrog.neobase.test.Person;
import de.whitefrog.neobase.test.PersonRepository;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestModels {
  public static PersonRepository repository;
  public static RelationshipRepository<Likes> relationships;

  @BeforeClass
  public static void before() {
    repository = new PersonRepository(TestSuite.service());
    relationships = TestSuite.service().repository(Likes.class);
  }
  
  @Test
  public void newInstancesInHashSet() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Set<Person> persons = new HashSet<>();
      persons.add(repository.createModel());
      persons.add(repository.createModel());
      assertThat(persons).hasSize(2);
      repository.save(persons.toArray(new Person[2]));
      persons = new HashSet<>(persons);
      Person first = repository.findByUuid(persons.iterator().next().getUuid());
      persons.add(first);
      assertThat(persons).hasSize(2);
    }
  }
}
