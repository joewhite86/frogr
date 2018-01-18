package de.whitefrog.froggy;

import de.whitefrog.froggy.test.TemporaryService;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestService.class,
  TestRepositories.class,
  TestModels.class
})
public class TestSuite {
  private static final Logger logger = LoggerFactory.getLogger(TestSuite.class);

  private static TemporaryService service;

  @BeforeClass
  public static void setup() throws ConfigurationException {
    service();
  }

  public static Service service() {
    if(service == null) {
      try {
        service = new TemporaryService();
        service.connect();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    return service;
  }
}
