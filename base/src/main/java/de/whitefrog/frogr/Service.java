package de.whitefrog.frogr;

import de.whitefrog.frogr.exception.FrogrException;
import de.whitefrog.frogr.model.Base;
import de.whitefrog.frogr.model.Graph;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.annotation.IndexType;
import de.whitefrog.frogr.patch.Patcher;
import de.whitefrog.frogr.persistence.AnnotationDescriptor;
import de.whitefrog.frogr.persistence.FieldDescriptor;
import de.whitefrog.frogr.persistence.ModelCache;
import de.whitefrog.frogr.persistence.Persistence;
import de.whitefrog.frogr.repository.GraphRepository;
import de.whitefrog.frogr.repository.ModelRepository;
import de.whitefrog.frogr.repository.Repository;
import de.whitefrog.frogr.repository.RepositoryFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Provides a service that handles the communication with frogr repositories and models.
 * Uses a embedded graph database and properties to set it up.
 * Automatically applies patches to the database if required, creates a new database
 * instance or uses an existing one and sets up the database scheme if required.
 */
public class Service implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Service.class);
  private static final String snapshotSuffix = "-SNAPSHOT";
  public static final String noVersion = "0.0.0";
  public enum State { Started, Connecting, Running, ShuttingDown}

  private GraphDatabaseService graphDb;
  private GraphRepository graphRepository;
  private Graph graph;
  private RepositoryFactory repositoryFactory;
  private Set<String> packageRegistry = new HashSet<>();
  private Validator validator;
  private String neo4jConfig = "config/neo4j.properties";
  private State state = State.Started;
  private Persistence persistence;
  private ModelCache modelCache;
  private String directory;

  public Service() {
    Locale.setDefault(Locale.GERMAN);
    register("de.whitefrog.frogr.model");
    register("de.whitefrog.frogr.repository");
  }

  public Transaction beginTx() {
    return graph().beginTx();
  }

  public ModelCache cache() {
    return modelCache;
  }

  public void connect() {
    if(isConnected()) throw new FrogrException("already running");
    state = State.Connecting;
    graphDb = createGraphDatabase();
    
    // create the model cache
    modelCache = new ModelCache();
    modelCache.scan(registry());
    
    persistence = new Persistence(this, modelCache);
    
    repositoryFactory = new RepositoryFactory(this);
    graphRepository = new GraphRepository(this);

    String version = getManifestVersion();
    
    try(Transaction tx = beginTx()) {
      graph = graphRepository.getGraph();
      if(logger.isInfoEnabled()) {
        String outputVersion = graph == null? version: (graph.getVersion() != null? graph.getVersion(): "");
        logger.info("Creating database instance {}", outputVersion);
        if(directory != null) logger.info("Graph location: {}", new File(directory).getAbsolutePath());
      }
      tx.success();
    }

    initializeSchema();
    Patcher.patch(this);

    try(Transaction tx = beginTx()) {
      if(graph == null) graph = graphRepository.create();
      graph.setVersion(version);
      graphRepository.save(graph);
      logger.debug("graph node created");
      tx.success();
    }

    registerShutdownHook(this);
    state = State.Running;
  }
  
  protected GraphDatabaseService createGraphDatabase() {
    PropertiesConfiguration config;
    File file;
    try {
      config = new PropertiesConfiguration(neo4jConfig);
      directory = config.containsKey("graph.location")? config.getString("graph.location"): "graph.db";
      file = new File(directory);
      GraphDatabaseBuilder builder = new GraphDatabaseFactory()
        .newEmbeddedDatabaseBuilder(file)
        .loadPropertiesFromURL(config.getURL());
      return builder.newGraphDatabase();
    } catch(ConfigurationException e) {
      // config not found
      if(!e.getMessage().startsWith("Cannot locate")) {
        throw new FrogrException(e.getMessage(), e);
      }
      directory = "graph.db";
      file = new File(directory);
      logger.info("No neo4j.properties found, creating graph in {}", file.getAbsolutePath());
      GraphDatabaseBuilder builder = new GraphDatabaseFactory()
        .newEmbeddedDatabaseBuilder(file);
      return builder.newGraphDatabase();
    }
  }
  
  public boolean isConnected() {
    return Arrays.asList(State.Connecting, State.Running, State.ShuttingDown)
      .contains(state);  
  }
  
  public void setConfig(String configFile) {
    neo4jConfig = configFile;
  }

  public String getVersion() {
    return graph != null? graph.getVersion(): Service.noVersion;
  }
  public void setVersion(String version) {
    if(graph == null) return;
    try(Transaction tx = beginTx()) {
      graph.setVersion(version);
      graphRepository.save(graph);
      tx.success();
    }
  }
  
  public State getState() {
    return state;
  }
  
  public Persistence persistence() {
    return persistence;
  }
  
  public Set<String> registry() {
    return packageRegistry;
  }
  public void register(String name) {
    packageRegistry.add(name);
  }

  private static void registerShutdownHook(final Service service) {
    Runtime.getRuntime().addShutdownHook(new Thread(service::shutdown));
  }

  @SuppressWarnings("unchecked")
  public <R extends Repository<T>, T extends Base> R repository(Class<T> clazz) {
    if(!isConnected()) throw new FrogrException("service is not running");
    return (R) repositoryFactory().get(clazz);
  }

  @SuppressWarnings("unchecked")
  public <R extends Repository> R repository(String name) {
    if(!isConnected()) throw new FrogrException("service is not running");
    return (R) repositoryFactory().get(name);
  }
  
  public RepositoryFactory repositoryFactory() {
    if(!isConnected()) throw new FrogrException("service is not running");
    return repositoryFactory;
  }

  public GraphDatabaseService graph() {
    if(!isConnected()) throw new FrogrException("service is not running");
    return graphDb;
  }

  public synchronized String getManifestVersion() {
    String version = null;
    
    if(getMainClass() != null) {
      version = getMainClass().getPackage().getImplementationVersion();
    }
    if(version == null) {
      version = System.getProperty("version", Service.noVersion);
    }

    if(version.endsWith(snapshotSuffix)) version = version.replace(snapshotSuffix, StringUtils.EMPTY);

    return version;
  }

  /**
   * Searches in stack trace for {@link Application} instances, otherwise returns this class
   * @return the main class of the application
   */
  private Class getMainClass() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for(int i = stackTrace.length - 1; i >= 0; i--) {
      try {
        Class caller = Class.forName(stackTrace[i].getClassName());
        if(Application.class.isAssignableFrom(caller)) 
          return caller;
        // if service is found first, no application did run 
        // the service as it should appear first in the stack trace
        if(Service.class.isAssignableFrom(caller)) 
          return caller;
      } catch(ClassNotFoundException e) {
        logger.debug("error reading stack trace", e);
      }
    }
    return getClass();
  }

  /**
   * 
   */
  @SuppressWarnings("unchecked")
  private void initializeSchema() {
    try(Transaction tx = beginTx()) {
      Schema schema = graph().schema();
      for(Class modelClass : cache().getAllModels()) {
        if(!Model.class.isAssignableFrom(modelClass) || Modifier.isAbstract(modelClass.getModifiers())) continue;
        if(!Base.class.isAssignableFrom(modelClass)) 
          throw new FrogrException("model class " + modelClass.getName() + " is not of type Base");

        if(logger.isDebugEnabled()) logger.debug("creating constraints for {}", modelClass.getSimpleName());

        ModelRepository repository = (ModelRepository) repository(modelClass);
        List<ConstraintDefinition> constraints = Iterables.asList(
          schema.getConstraints(repository.label()));
        List<IndexDefinition> indices = Iterables.asList(
          schema.getIndexes(repository.label()));
        
        for(FieldDescriptor descriptor : cache().fieldMap(modelClass)) {
          AnnotationDescriptor annotations = descriptor.annotations();
          ConstraintDefinition existingConstraint = null;
          String indexName = descriptor.getName() + 
            (annotations.indexed != null && annotations.indexed.type().equals(IndexType.LowerCase)? "_lower": "");
          
          for(ConstraintDefinition constraint : constraints) {
            String property = Iterables.single(constraint.getPropertyKeys());
            if(property.equals(indexName)) {
              existingConstraint = constraint;
              break;
            }
          }
          IndexDefinition existingIndex = null;
          for(IndexDefinition index : indices) {
            String property = Iterables.single(index.getPropertyKeys());
            if(property.equals(indexName)) {
              existingIndex = index;
              break;
            }
          }

          if(annotations.unique && existingConstraint == null) {
            schema.constraintFor(repository.label())
              .assertPropertyIsUnique(indexName)
              .create();
            logger.debug("created unique constraint on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          } else if(!annotations.unique && existingConstraint != null) {
            existingConstraint.drop();
            logger.debug("dropped unique constraint on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          }

          if(annotations.indexed != null && !annotations.unique && existingIndex == null) {
            schema.indexFor(repository.label())
              .on(indexName)
              .create();
            logger.debug("created {} index on field \"{}\" for model \"{}\"",
              annotations.indexed.type(), descriptor.getName(), repository.getModelClass().getSimpleName());
          } else if(annotations.indexed == null && !annotations.unique && existingIndex != null && existingConstraint == null) {
            existingIndex.drop();
            logger.debug("dropped index on field \"{}\" for model \"{}\"",
              descriptor.getName(), repository.getModelClass().getSimpleName());
          }
        }
      }
//      schema.awaitIndexesOnline(Long.MAX_VALUE, TimeUnit.SECONDS);
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
  public void close() {
    shutdown();
  }

  public void shutdown() {
    state = State.ShuttingDown;
    if(repositoryFactory() != null) repositoryFactory().cache().forEach(Repository::dispose);
    if(graphDb != null) graphDb.shutdown();
    state = State.Started;
  }
}
