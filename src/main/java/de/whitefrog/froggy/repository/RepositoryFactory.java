package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.exception.RepositoryInstantiationException;
import de.whitefrog.froggy.exception.RepositoryNotFoundException;
import de.whitefrog.froggy.model.relationship.Relationship;
import de.whitefrog.froggy.persistence.Persistence;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory to build repository instances.
 * Uses a static and a local cache to prevent from double allocations.
 * Works in a multi-thread environment with different services running at the same time.
 */
public class RepositoryFactory {
  
  private static final Logger logger = LoggerFactory.getLogger(RepositoryFactory.class);
  /**
   * Static repository cache by name and service.
   */
  private static final Map<Service, Map<String, Repository>> staticCache = new HashMap<>();
  /**
   * Static repository class cache.
   */
  private static final Map<String, Class> repositoryCache = new HashMap<>();

  /**
   * Local service.
   */
  private final Service service;
  /**
   * Local repository cache.
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
   * Return all currently cached repository classes.
   * @return All currently cached repository classes
   */
  public Collection<Class> repositoryClasses() {
    return repositoryCache.values();
  }

  /**
   * Return all currently cached repositories.
   * @return All currently cached repositories
   */
  public Collection<Repository> cache() {
    return cache.values();
  }

  /**
   * Get the repository for a specific model class.
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
        Constructor<Repository> ctor = ConstructorUtils.getMatchingAccessibleConstructor(c, new Class[] {service.getClass()});
        if (ctor == null) {
          throw new RepositoryInstantiationException("No constructor " + name + "Repository(" 
            + service.getClass().getName() + ") found in " + c.getName());
        }
        repository = ctor.newInstance(service);
      } catch(ClassNotFoundException e) {
        try {
          logger.debug("No repository found for " + modelClass.getSimpleName() + ", creating a default one");
          if(!Relationship.class.isAssignableFrom(modelClass)) {
            Constructor<DefaultRepository> ctor = 
              DefaultRepository.class.getConstructor(Service.class, String.class);
            repository = ctor.newInstance(service, name);
          } else {
            Constructor<DefaultRelationshipRepository> ctor = 
              DefaultRelationshipRepository.class.getConstructor(Service.class, String.class);
            repository = ctor.newInstance(service, name);
          }
        } catch(ReflectiveOperationException ex) {
          throw new RepositoryInstantiationException(e.getCause());
        }
      } catch(ReflectiveOperationException e) {
        throw new RepositoryInstantiationException(e.getCause());
      }
      logger.debug("registering " + repository.getClass().getSimpleName() + " for " + name);
      cache.put(name.toLowerCase(), repository);
    }

    return repository;
  }

  /**
   * Get the repository for the model with a specific name.
   * @param name Model name used to lookup the repository
   * @return Repository used for the passed model name
   */
  @SuppressWarnings("unchecked")
  public Repository get(String name) {
    if(name == null) throw new NullPointerException("name cannot be null");
    if(cache.containsKey(name.toLowerCase())) {
      return cache.get(name.toLowerCase());
    }
    Class clazz = Persistence.cache().getModel(name);
    if(clazz == null) {
      throw new RepositoryNotFoundException(name + " not found in model cache");
    }
    return get(clazz);
  }

  /**
   * Register a new repository manually. It should not be neccessary to call
   * this under normal circumstances. Use the service registry appropriatly.
   * @param name Model name used in the repository
   * @param repository Repository to add to the cache
   */
  public void register(String name, Repository repository) {
    cache.put(name, repository);
  }
}

