package de.whitefrog.frogr.rest.service;

import com.codahale.metrics.MetricRegistry;
import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.Model;
import de.whitefrog.frogr.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;

/**
 * Provides basic REST service functionality common to most services.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class RestService<Repo extends Repository<M>, M extends Model> {
  private static final Logger logger = LoggerFactory.getLogger(RestService.class);
  private Repo repository = null;
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
}
