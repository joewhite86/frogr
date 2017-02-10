package de.whitefrog.neobase;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Graph;
import de.whitefrog.neobase.patch.Patcher;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.GraphRepository;
import de.whitefrog.neobase.repository.Repository;
import de.whitefrog.neobase.repository.RepositoryFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Service implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Service.class);
  private static final String neo4jConfig = "config/neo4j.properties";
  
  private GraphDatabaseService graphDb;
  private String directory;
  private GraphRepository graphRepository;
  private Graph graph;
  private RepositoryFactory repositoryFactory;
  private Set<String> packageRegistry = new HashSet<>();
  private Validator validator;
  private boolean connected = false;
  private boolean useBolt = true;

  public Service() {
    Locale.setDefault(Locale.GERMAN);
  }

  public Transaction beginTx() {
    return graph().beginTx();
  }

  public void connect(String directory) {
    try {
      logger.info("connecting to cypher at {}", directory);
      this.directory = directory;
      GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector( "0" );
      GraphDatabaseBuilder builder = new GraphDatabaseFactory()
        .newEmbeddedDatabaseBuilder(new File(directory))
        .loadPropertiesFromURL(new PropertiesConfiguration(neo4jConfig).getURL());
      if(useBolt) {
        builder
          .setConfig(bolt.enabled, "true")
          .setConfig(bolt.address, "localhost:7600");
      }
      graphDb = builder.newGraphDatabase();

      Persistence.setService(this);
      repositoryFactory = new RepositoryFactory(this);
      graphRepository = new GraphRepository(this);
      try(Transaction tx = beginTx()) {
        graph = graphRepository.getGraph();
        if(graph == null) {
          logger.info("--------------------------------------------");
          logger.info("---   creating fresh database instance   ---");
          logger.info("---   " + directory + "   ---");
          logger.info("--------------------------------------------");
          graph = graphRepository.create();
        }
        tx.success();
      }

      Patcher.patch(this);

      registerShutdownHook(this);
      connected = true;
    } catch (ConfigurationException e) {
      logger.error("Could not read cypher.properties", e);
    }
  }

  public void connect() {
    try {
      Configuration properties = new PropertiesConfiguration("config/myband.properties");
      String directory = properties.getString("graph.location");
      connect(directory);
    } catch (ConfigurationException e) {
      logger.error("Could not read myband.properties", e);
    }
  }
  
  public boolean isConnected() {
    return connected;
  }
  
  public String directory() {
    return directory;
  }

  public String getVersion() {
    return graph != null? graph.getVersion(): null;
  }
  public void setVersion(String version) {
    graph.setVersion(version);
    graphRepository.save(graph);
  }
  
  public Set<String> registry() {
    return packageRegistry;
  }
  public void register(String name) {
    packageRegistry.add(name);
  }

  private static void registerShutdownHook(final Service service) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        service.shutdown();
      }
    });
  }

  @SuppressWarnings("unchecked")
  public <R extends Repository<T>, T extends Base> R repository(Class<T> clazz) {
    return (R) repositoryFactory().get(clazz);
  }

  @SuppressWarnings("unchecked")
  public <R extends Repository> R repository(String name) {
    return (R) repositoryFactory().get(name);
  }
  
  public RepositoryFactory repositoryFactory() {
    return repositoryFactory;
  }

  public GraphDatabaseService graph() {
    if(graphDb == null) connect();
    return graphDb;
  }

  public Validator validator() {
    if(validator == null) {
      validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
    return validator;
  }

  @Override
  public void close() throws Exception {
    shutdown();
  }

  public void shutdown() {
    repositoryFactory().cache().forEach(Repository::dispose);
    if(graphDb != null) graphDb.shutdown();
  }
}
