package de.whitefrog.frogr;

import de.whitefrog.frogr.exception.DuplicateEntryException;
import de.whitefrog.frogr.exception.MissingRequiredException;
import de.whitefrog.frogr.exception.RepositoryInstantiationException;
import de.whitefrog.frogr.repository.DefaultRepository;
import de.whitefrog.frogr.repository.ModelRepository;
import de.whitefrog.frogr.repository.RelationshipRepository;
import de.whitefrog.frogr.repository.Repository;
import de.whitefrog.frogr.test.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRepositories {
  private static PersonRepository persons;
  private static RelationshipRepository<Likes> likesRepository;
  
  @BeforeClass
  public static void before() {
    persons = TestSuite.service().repository(Person.class);
    likesRepository = TestSuite.service().repository(Likes.class);
  }
  
  @Test
  public void correctLabel() {
    assertThat(persons.label().name()).isEqualTo("Person");
  }
  
  @Test
  public void createModel() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = persons.createModel();
      model.setField("test");
      persons.save(model);
      model = persons.find(model.getId(), "field");
      assertThat(model).isNotNull();
      assertThat(model.getField()).isEqualTo("test");
    }
  }
  
  @Test(expected = MissingRequiredException.class)
  public void createModelMissingRequired() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      ModelRepository<PersonRequiredField> repository = TestSuite.service().repository(PersonRequiredField.class);
      PersonRequiredField model = repository.createModel();
      repository.save(model);
    }
  }
  
  @Test
  public void uuid() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = persons.createModel();
      persons.save(model);
      assertThat(model.getUuid()).isNotEmpty();
    }
  }
  
  @Test(expected = DuplicateEntryException.class)
  public void uniqueConstraint() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model = persons.createModel();
      model.setUniqueField("unique");
      persons.save(model);
      Person duplicate = persons.createModel();
      duplicate.setUniqueField("unique");
      persons.save(duplicate);
    }
  }
  
  @Test
  public void createRelationship() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = persons.createModel();
      model1.setField("test1");
      Person model2 = persons.createModel();
      model2.setField("test2");
      persons.save(model1, model2);
      
      Likes likes = new Likes(model1, model2);
      likes.setField("test");
      likesRepository.save(likes);
      likes = likesRepository.find(likes.getId(), "from", "to", "field");
      assertThat(likes).isNotNull();
      assertThat(likes.getField()).isEqualTo("test");
      assertThat(likes.getFrom()).isEqualTo(model1);
      assertThat(likes.getTo()).isEqualTo(model2);
    }
  }

  @Test
  public void createRelationship2() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = persons.createModel();
      Person model2 = persons.createModel();
      persons.save(model1, model2);

      model1.setLikes(new ArrayList<>());
      model1.getLikes().add(model2);
      persons.save(model1);
      model1 = persons.find(model1.getId(), "likes");
      assertThat(model1).isNotNull();
      assertThat(model1.getLikes()).hasSize(1);
      assertThat(model1.getLikes().get(0)).isEqualTo(model2);
    }
  }

  // There will not be an exception, else we run into problems
  // on not named relationships that already exist
  @Ignore
  @Test(expected = DuplicateEntryException.class)
  public void createDuplicateRelationship() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person model1 = persons.createModel();
      Person model2 = persons.createModel();
      persons.save(model1, model2);

      model1.setLikes(new ArrayList<>());
      model1.getLikes().add(model2);
      persons.save(model1);
      
      model1.getLikes().clear();
      model1.getLikes().add(model2);
      persons.save(model1);
      
      Likes duplicate = new Likes(model1, model2);
      duplicate.setField("test");
      likesRepository.save(duplicate);
    }
  }
  
  @Test
  public void findByUuid() {
    try(Transaction tx = TestSuite.service().beginTx()) {
      Person person = persons.createModel();
      persons.save(person);
      assertThat(person.getUuid()).isNotEmpty();
      Person found = persons.findByUuid(person.getUuid());
      assertThat(found).isEqualTo(person);
    }
  }
  
  @Test(expected = RepositoryInstantiationException.class)
  public void invalidRepository() {
    TestSuite.service().repository("Invalid");
  }
  
  @Test
  public void defaultRepository() {
    Repository<Clothing> repository = TestSuite.service().repository(Clothing.class);
    assertThat(repository).isInstanceOfAny(DefaultRepository.class);
    assertThat(TestSuite.service().repositoryFactory().cache()).contains(repository);
  }
  
  @Test
  public void repositoryCache() {
    assertThat(TestSuite.service().repositoryFactory().cache()).contains(likesRepository);
  }
}
