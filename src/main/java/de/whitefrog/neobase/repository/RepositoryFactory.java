package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.exception.RepositoryInstantiationException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RepositoryFactory {
  private static final Map<Service, Map<String, Repository>> staticCache = new HashMap<>();
  private static final Map<String, Class> classCache = new HashMap<>();
  
  private final Service service;
  private final Map<String, Repository> cache;
  
  public RepositoryFactory(Service service) {
    this.service = service;
    if(staticCache.containsKey(service)) {
      this.cache = staticCache.get(service);
    } else {
      staticCache.put(service, new HashMap<>());
      this.cache = staticCache.get(service);
    }
    if(classCache.isEmpty()) {
      for(String pkg : service.registry()) {
        Reflections reflections = new Reflections(pkg);
        for(Class clazz : reflections.getSubTypesOf(Repository.class)) {
          classCache.put(clazz.getSimpleName(), clazz);
        }
      }
    }
  }
  
  public Collection<Repository> cache() {
    return cache.values();
  }
  
  public Repository get(Class modelType) {
    return get(modelType.getSimpleName());
  }

  @SuppressWarnings("unchecked")
  public Repository get(String name) {
    Repository repository;

    if(cache.containsKey(name.toLowerCase())) {
      repository = cache.get(name.toLowerCase());
    } else {
      try {
        if(!classCache.containsKey(name + "Repository")) {
          throw new ClassNotFoundException(name + "Repository");
        }
        Class c = classCache.get(name + "Repository");
        Constructor<Repository> ctor = c.getConstructor(Service.class);
        repository = ctor.newInstance(service);
      } catch(ClassNotFoundException | NoSuchMethodException e) {
        try {
          Constructor<DefaultRepository> ctor = DefaultRepository.class.getConstructor(Service.class, String.class);
          repository = ctor.newInstance(service, name);
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

  public GraphDatabaseService getDatabase() {
    return service.graph();
  }
}

