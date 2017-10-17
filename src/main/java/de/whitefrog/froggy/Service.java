package de.whitefrog.froggy;

import de.whitefrog.froggy.model.Base;
import de.whitefrog.froggy.model.Graph;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.patch.Patcher;
import de.whitefrog.froggy.persistence.AnnotationDescriptor;
import de.whitefrog.froggy.persistence.FieldDescriptor;
import de.whitefrog.froggy.persistence.Persistence;
import de.whitefrog.froggy.repository.GraphRepository;
import de.whitefrog.froggy.repository.ModelRepository;
import de.whitefrog.froggy.repository.Repository;
import de.whitefrog.froggy.repository.RepositoryFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides a service that handles the communication with froggy repositories and models.
 * Uses a embedded graph database and properties to set it up.
 * Automatically applies patches to the database if required, creates a new database
 * instance or uses an existing one and sets up the database scheme if required.
 */
public class Service implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Service.class);
  private static final String neo4jConfig = "config/neo4j.properties";
  private static final String snapshotSuffix = "-SNAPSHOT";
  private static String version;
  protected enum State { Started, Running, ShuttingDown }
  
  private GraphDatabaseService graphDb;
  private String directory;
  private GraphRepository graphRepository;
  private Graph graph;
  private RepositoryFactory repositoryFactory;
  private Set<String> packageRegistry = new HashSet<>();
  private Validator validator;
  private boolean connected = false;
  private boolean useBolt = true;
  private State state = State.Started;

  public Service() {
    Locale.setDefault(Locale.GERMAN);
  }

  public Transaction beginTx() {
    return graph().beginTx();
  }

  public void connect(String directory) {
    try {
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
          graph = graphRepository.create();
          graph.setVersion(getManifestVersion());
          graphRepository.save(graph);
          logger.info("--------------------------------------------");
          logger.info("---   creating fresh database instance   ---");
          logger.info("---   " + directory + "   ---");
          logger.info("--------------------------------------------");
        } else {
          graph.setVersion(getManifestVersion());
          graphRepository.save(graph);
          logger.info("--------------------------------------------");
          logger.info("---   starting database instance {}   ---", graph.getVersion());
          logger.info("---   {}   ---", directory);
          logger.info("--------------------------------------------");
        }
        tx.success();
      }

      Patcher.patch(this);
      initializeSchema();

      registerShutdownHook(this);
      connected = true;
      state = State.Running;
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
  
  public State getState() {
    return state;
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

  public synchronized static String getManifestVersion() {
    if(version != null) return version;

    version = System.getProperty("version", Patcher.class.getPackage().getImplementationVersion());

    if(version == null) {
      version = "undefined";
      logger.warn("No implementation version found in manifest");
    } else {
      if(version.endsWith(snapshotSuffix)) version = version.replace(snapshotSuffix, StringUtils.EMPTY);
    }

    return version;
  }
  
  private void initializeSchema() {
    try(Transaction tx = beginTx()) {
      for(Class modelClass : Persistence.cache().getAllModels()) {
        if(!Model.class.isAssignableFrom(modelClass) || Modifier.isAbstract(modelClass.getModifiers())) continue;

        ModelRepository repository = (ModelRepository) repository(modelClass);
        List<ConstraintDefinition> constraints = Iterables.asList(
          graph().schema().getConstraints(repository.label()));
        List<IndexDefinition> indices = Iterables.asList(
          graph().schema().getIndexes(repository.label()));
        
        for(FieldDescriptor descriptor : Persistence.cache().fieldMap(modelClass)) {
          AnnotationDescriptor annotations = descriptor.annotations();
          ConstraintDefinition existing = null;
          for(ConstraintDefinition constraint : constraints) {
            String property = Iterables.single(constraint.getPropertyKeys());
            if(property.equals(descriptor.getName())) {
              existing = constraint;
              break;
            }
          }
          IndexDefinition existingIndex = null;
          for(IndexDefinition index : indices) {
            String property = Iterables.single(index.getPropertyKeys());
            if(property.equals(descriptor.getName())) {
              existingIndex = index;
              break;
            }
          }          
          
          if(annotations.unique && existing == null) {
            graph().schema().constraintFor(repository.label())
              .assertPropertyIsUnique(descriptor.getName())
              .create();
            logger.info("created unique constraint on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          } else if(!annotations.unique && existing != null) {
            existing.drop();
            logger.info("dropped unique constraint on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          }

          if(annotations.indexed != null && !annotations.unique && existingIndex == null) {
            graph().schema().indexFor(repository.label())
              .on(descriptor.getName())
              .create();
            logger.info("created index on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          } else if(annotations.indexed == null && !annotations.unique && existingIndex != null) {
            existingIndex.drop();
            logger.info("dropped index on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          }
        }
      }
      tx.success();
    }
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
    state = State.ShuttingDown;
    repositoryFactory().cache().forEach(Repository::dispose);
    if(graphDb != null) graphDb.shutdown();
  }
}
