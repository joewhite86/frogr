package de.whitefrog.froggy.rest.service;

import com.codahale.metrics.MetricRegistry;
import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.helper.ReflectionUtil;
import de.whitefrog.froggy.model.Model;
import de.whitefrog.froggy.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;

/**
 * Provides basic REST service functionality common to most services.
 */
@Produces(MediaType.APPLICATION_JSON)
public class RestService<Repo extends Repository<M>, M extends Model> {
  private static final Logger logger = LoggerFactory.getLogger(RestService.class);
  private Repo repository = null;
  private Class repositoryClass;
  private Class modelClass;
  public static final MetricRegistry metrics = new MetricRegistry();

  @Inject
  private Service service;

  public Service service() {
    return service;
  }

  @SuppressWarnings("unchecked")
  public Repo repository() {
    if(repository == null) {
      try {
        repository = (Repo) service().repository(getModelClass());
      } catch(ClassNotFoundException e) {
        logger.error("class for service {}", this.getClass().getName());
      }
    }
    return repository;
  }

  private Class getModelClass() throws ClassNotFoundException {
    if(modelClass == null) {
      Type[] parameterizedTypes = ReflectionUtil.getParameterizedTypes(this);
      modelClass = ReflectionUtil.getClass(parameterizedTypes[1]);
    }
    return modelClass;
  }

  private Class getRepositoryClass() throws ClassNotFoundException {
    if(repositoryClass == null) {
      Type[] parameterizedTypes = ReflectionUtil.getParameterizedTypes(this);
      repositoryClass = ReflectionUtil.getClass(parameterizedTypes[0]);
    }
    return repositoryClass;
  }
}
