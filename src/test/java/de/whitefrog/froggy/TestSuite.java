package de.whitefrog.froggy;

import de.whitefrog.froggy.repository.Repository;
import de.whitefrog.froggy.test.Likes;
import de.whitefrog.froggy.test.Person;
import de.whitefrog.froggy.test.PersonRepository;
import de.whitefrog.froggy.test.TemporaryService;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestService.class,
  TestRepositories.class,
  TestModels.class,
  TestSearch.class
})
public class TestSuite {
  private static final Logger logger = LoggerFactory.getLogger(TestSuite.class);

  private static TemporaryService service;
  public static Person person1;
  public static Person person2;

  @BeforeClass
  public static void setup() {
    service();
  }

  public static Service service() {
    if(service == null) {
      try {
        service = new TemporaryService();
        service.connect();
        prepareData();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    return service;
  }
  
  private static void prepareData() {
    PersonRepository persons = service().repository(Person.class);
    Repository<Likes> likesRepository = service().repository(Likes.class);
    
    try(Transaction tx = service().beginTx()) {
      person1 = persons.createModel();
      person1.setField("test1");
      person1.setUniqueField("test1");
      person1.setNumber(10L);
      
      person2 = persons.createModel();
      person2.setField("test2");
      person2.setUniqueField("test2");
      person2.setNumber(20L);
      
      persons.save(person1, person2);

      Likes likes1 = new Likes(person1, person2);
      Likes likes2 = new Likes(person2, person1);
      
      likesRepository.save(likes1, likes2);

      tx.success();
    }
  }
}
