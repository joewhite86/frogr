package de.whitefrog.frogr.example;

import de.whitefrog.frogr.Service;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService extends Service {
  private static final Logger logger = LoggerFactory.getLogger(MyService.class);
  private static final String appConfig = "config/myapp.properties";

  private Configuration config;

  @Override
  public void connect() {
    try {
      config = new PropertiesConfiguration(appConfig);
      super.connect();
    } catch(ConfigurationException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public Configuration config() { return config; }
}