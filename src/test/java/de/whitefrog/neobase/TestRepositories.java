package de.whitefrog.neobase;

import de.whitefrog.neobase.exception.DuplicateEntryException;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.repository.RelationshipRepository;
import de.whitefrog.neobase.test.Likes;
import de.whitefrog.neobase.test.Person;
import de.whitefrog.neobase.test.PersonRepository;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRepositories {
  public static PersonRepository repository;
  public static RelationshipRepository<Likes> relationships;
  
  @BeforeClass
  public static void before() {
    repository = new PersonRepository(TestSuite.service());
    relationships = TestSuite.service().repository(Likes.class);
  }
  
  @Test
  public void correctLabel() {
    assertThat(repository.label().name()).isEqualTo("Person");
  }
  
  @Test
  public void createModel() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = repository.createModel();
      model.setField("test");
      repository.save(model);
      model = repository.find(model.getId(), "field");
      assertThat(model).isNotNull();
      assertThat(model.getField()).isEqualTo("test");
    }
  }
  
  @Test
  public void uuid() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = repository.createModel();
      repository.save(model);
      assertThat(model.getUuid()).isNotEmpty();
    }
  }
  
  @Test(expected = DuplicateEntryException.class)
  public void uniqueConstraint() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = repository.createModel();
      model.setUniqueField("unique");
      repository.save(model);
      Person duplicate = repository.createModel();
      duplicate.setUniqueField("unique");
      repository.save(duplicate);
    }
  }
  
  @Test
  public void createRelationship() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = repository.createModel();
      model1.setField("test1");
      Person model2 = repository.createModel();
      model2.setField("test2");
      repository.save(model1, model2);
      
      Likes likes = new Likes();
      likes.setFrom(model1);
      likes.setTo(model2);
      likes.setField("test");
      relationships.save(likes);
      likes = relationships.find(likes.getId(), "from", "to", "field");
      assertThat(likes).isNotNull();
      assertThat(likes.getField()).isEqualTo("test");
      assertThat(likes.getFrom()).isEqualTo(model1);
      assertThat(likes.getTo()).isEqualTo(model2);
    }
  }

  @Test
  public void createRelationship2() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = repository.createModel();
      Person model2 = repository.createModel();
      repository.save(model1, model2);

      model1.setLikes(new ArrayList<>());
      model1.getLikes().add(model2);
      repository.save(model1);
      model1 = repository.find(model1.getId(), "likes");
      assertThat(model1).isNotNull();
      assertThat(model1.getLikes()).hasSize(1);
      assertThat(model1.getLikes().get(0)).isEqualTo(model2);
    }
  }

  @Ignore
  @Test(expected = DuplicateEntryException.class)
  public void createDuplicateRelationship() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = repository.createModel();
      Person model2 = repository.createModel();
      repository.save(model1, model2);

      model1.getLikes().add(model2);
      repository.save(model1);
      
      model1.getLikes().clear();
      model1.getLikes().add(model2);
      repository.save(model1);

      Likes duplicate = new Likes();
      duplicate.setFrom(model1);
      duplicate.setTo(model2);
      duplicate.setField("test");
      relationships.save(duplicate);
    }
  }
  
  @Test
  public void search() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person person1 = repository.createModel();
      Person person2 = repository.createModel();
      repository.save(person1, person2);

      Likes likes1 = new Likes();
      likes1.setFrom(person1);
      likes1.setTo(person2);
      
      Likes likes2 = new Likes();
      likes2.setFrom(person2);
      likes2.setTo(person1);
      relationships.save(likes1, likes2);
      
      List<Person> persons = repository.search()
        .filter(new Filter.StartsWith("field", "test"))
        .list();
    }
  }
}
