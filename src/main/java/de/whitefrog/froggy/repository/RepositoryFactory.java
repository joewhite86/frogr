package de.whitefrog.froggy.repository;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.exception.RepositoryInstantiationException;
import de.whitefrog.froggy.exception.RepositoryNotFoundException;
import de.whitefrog.froggy.model.relationship.Relationship;
import de.whitefrog.froggy.persistence.Persistence;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RepositoryFactory {
  private static final Map<Service, Map<String, Repository>> staticCache = new HashMap<>();
  private static final Map<String, Class> repositoryCache = new HashMap<>();
  
  private final Service service;
  private final Map<String, Repository> cache;
  
  public RepositoryFactory(Service service) {
    this.service = service;
    if(!staticCache.containsKey(service)) {
      staticCache.put(service, new HashMap<>());
    }
    this.cache = staticCache.get(service);
    if(repositoryCache.isEmpty()) {
      for(String pkg : service.registry()) {
        Reflections reflections = new Reflections(pkg);
        for(Class clazz : reflections.getSubTypesOf(Repository.class)) {
          repositoryCache.put(clazz.getSimpleName(), clazz);
        }
      }
    }
  }
  
  public Collection<Class> repositoryClasses() {
    return repositoryCache.values();
  }
  
  public Collection<Repository> cache() {
    return cache.values();
  }
  
  public Repository get(Class modelType) {
    Repository repository;
    String name = modelType.getSimpleName();

    if(cache.containsKey(name.toLowerCase())) {
      repository = cache.get(name.toLowerCase());
    } else {
      try {
        if(!repositoryCache.containsKey(name + "Repository")) {
          throw new ClassNotFoundException(name + "Repository");
        }
        Class c = repositoryCache.get(name + "Repository");
        Constructor<Repository> ctor = ConstructorUtils.getMatchingAccessibleConstructor(c, new Class[] {service.getClass()});
        repository = ctor.newInstance(service);
      } catch(ClassNotFoundException e) {
        try {
          if(!Relationship.class.isAssignableFrom(modelType)) {
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
      cache.put(name.toLowerCase(), repository);
    }

    return repository;
  }

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

  public GraphDatabaseService getDatabase() {
    return service.graph();
  }

  public void register(String name, Repository repository) {
    cache.put(name, repository);
  }
}

