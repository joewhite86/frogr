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
   * Static {@link Repository repository} {@link Class class} cache.
   */
  private final Map<String, Class> repositoryCache = new HashMap<>();

  /**
   * Local {@link Service service}.
   */
  private final Service service;
  /**
   * Local {@link Repository repository} cache.
   */
  private final Map<String, Repository> cache = new HashMap<>(100);
  
  public RepositoryFactory(Service service) {
    this.service = service;
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
    return get(service.cache().getModelName(modelClass));
  }


  /**
   * Get the {@link Repository repository} for the {@link Model model} with a specific name.
   * @param name Model name used to lookup the repository
   * @return Repository used for the passed model name
   */
  public Repository get(String name) {
    Repository repository;

    try {
      if(cache.containsKey(name)) {
        repository = cache.get(name);
      }
      else if(!repositoryCache.containsKey(name + "Repository")) {
        logger.debug("No repository found for {}, creating a default one", name);
        Class modelClass = service.cache().getModel(name);
        if(modelClass == null) {
          throw new RepositoryNotFoundException(name);
        }
        if(!Relationship.class.isAssignableFrom(modelClass)) {
          Constructor<DefaultModelRepository> ctor =
            DefaultModelRepository.class.getConstructor(String.class);
          repository = ctor.newInstance(name);
        } else {
          Constructor<DefaultRelationshipRepository> ctor =
            DefaultRelationshipRepository.class.getConstructor(String.class);
          repository = ctor.newInstance(name);
        }
        setRepositoryService(repository);
        cache.put(name, repository);
      }
      else {
        Class c = repositoryCache.get(name + "Repository");
        Constructor<Repository> ctor = c.getDeclaredConstructor();
        if(ctor == null) {
          throw new RepositoryInstantiationException("No constructor " + name + "Repository() found in " + c.getName());
        }
        repository = ctor.newInstance();
        setRepositoryService(repository);
        logger.debug("registering " + repository.getClass().getSimpleName() + " for " + name);
      }
      cache.put(name, repository);
    } catch(ReflectiveOperationException e) {
      throw new RepositoryInstantiationException(e);
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
   * Register a new {@link Repository repository} manually. It should not be neccessary to call
   * this under normal circumstances. Use {@link Service#register(String) the service registry} instead.
   * @param name {@link Model} name used in the repository
   * @param repository Repository to add to the cache
   */
  public void register(String name, Repository repository) {
    cache.put(name, repository);
  }
}

