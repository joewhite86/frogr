package de.whitefrog.frogr.repository;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.exception.RepositoryInstantiationException;
import de.whitefrog.frogr.exception.RepositoryNotFoundException;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.model.relationship.Relationship;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory to build {@link Repository repository} instances.
 * Uses a static and a local cache to prevent from double allocations.
 * Works in a multi-threaded environment with different {@link Service services} running at the same time.
 */
public class RepositoryFactory {
  
  private static final Logger logger = LoggerFactory.getLogger(RepositoryFactory.class);
  /**
   * Static {@link Repository repository} cache by name and {@link Service service}.
   */
  private static final Map<Service, Map<String, Repository>> staticCache = new HashMap<>();
  /**
   * Static {@link Repository repository} {@link Class class} cache.
   */
  private static final Map<String, Class> repositoryCache = new HashMap<>();

  /**
   * Local {@link Service service}.
   */
  private final Service service;
  /**
   * Local {@link Repository repository} cache.
   */
  private final Map<String, Repository> cache;
  
  public RepositoryFactory(Service service) {
    this.service = service;
    if(!staticCache.containsKey(service)) {
      staticCache.put(service, new HashMap<>());
    }
    this.cache = staticCache.get(service);
    if(repositoryCache.isEmpty()) {
      ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
        .setScanners(new SubTypesScanner());
      service.registry()
        .forEach(pkg -> configurationBuilder.addUrls(ClasspathHelper.forPackage(pkg)));
      Reflections reflections = new Reflections(configurationBuilder);
      for(Class clazz : reflections.getSubTypesOf(Repository.class)) {
        if (!Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())) {
          repositoryCache.put(clazz.getSimpleName(), clazz);
        }
      }
    }
  }

  /**
   * Return all currently cached {@link Repository repository} {@link Class classes}.
   * @return All currently cached repository classes
   */
  public Collection<Class> repositoryClasses() {
    return repositoryCache.values();
  }

  /**
   * Return all currently cached {@link Repository repositories}.
   * @return All currently cached repositories
   */
  public Collection<Repository> cache() {
    return cache.values();
  }

  /**
   * Get the {@link Repository repository} for a specific {@link Model model} {@link Class class}.
   * @param modelClass Model class used to lookup the repository
   * @return Repository used for the passed model class
   */
  public Repository get(Class modelClass) {
    Repository repository;
    String name = modelClass.getSimpleName();

    if(cache.containsKey(name.toLowerCase())) {
      repository = cache.get(name.toLowerCase());
    } else {
      try {
        if(!repositoryCache.containsKey(name + "Repository")) {
          throw new ClassNotFoundException(name + "Repository");
        }
        Class c = repositoryCache.get(name + "Repository");
        Constructor<Repository> ctor = c.getDeclaredConstructor();
        if (ctor == null) {
          throw new RepositoryInstantiationException("No constructor " + name + "Repository() found in " + c.getName());
        }
        repository = ctor.newInstance();
        setRepositoryService(repository);
      } catch(ClassNotFoundException e) {
        try {
          logger.debug("No repository found for " + modelClass.getSimpleName() + ", creating a default one");
          if(!Relationship.class.isAssignableFrom(modelClass)) {
            Constructor<DefaultRepository> ctor = 
              DefaultRepository.class.getConstructor(String.class);
            repository = ctor.newInstance(name);
          } else {
            Constructor<DefaultRelationshipRepository> ctor = 
              DefaultRelationshipRepository.class.getConstructor(String.class);
            repository = ctor.newInstance(name);
          }
          setRepositoryService(repository);
        } catch(ReflectiveOperationException ex) {
          throw new RepositoryInstantiationException(e);
        }
      } catch(ReflectiveOperationException e) {
        throw new RepositoryInstantiationException(e.getCause());
      }
      logger.debug("registering " + repository.getClass().getSimpleName() + " for " + name);
      cache.put(name.toLowerCase(), repository);
    }

    return repository;
  }

  public void setRepositoryService(Repository repository) throws ReflectiveOperationException {
    Field serviceField = BaseRepository.class.getDeclaredField("service");
    serviceField.setAccessible(true);
    serviceField.set(repository, service);
    BaseRepository.class.getDeclaredMethod("initialize").invoke(repository);
  }

  /**
   * Get the {@link Repository repository} for the {@link Model model} with a specific name.
   * @param name Model name used to lookup the repository
   * @return Repository used for the passed model name
   */
  @SuppressWarnings("unchecked")
  public Repository get(String name) {
    if(name == null) throw new NullPointerException("name cannot be null");
    if(cache.containsKey(name.toLowerCase())) {
      return cache.get(name.toLowerCase());
    }
    Class clazz = service.persistence().cache().getModel(name);
    if(clazz == null) {
      throw new RepositoryNotFoundException(name);
    }
    return get(clazz);
  }

  /**
   * Register a new {@link Repository repository} manually. It should not be neccessary to call
   * this under normal circumstances. Use {@link Service#register(String) the service registry} instead.
   * @param name {@link Model} name used in the repository
   * @param repository Repository to add to the cache
   */
  public void register(String name, Repository repository) {
    cache.put(name, repository);
  }
}

