package de.whitefrog.frogr.rest.service;

import com.codahale.metrics.MetricRegistry;
import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.helper.ReflectionUtil;
import de.whitefrog.frogr.model.Base;
import de.whitefrog.frogr.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Type;

/**
 * Provides basic REST service functionality common to most services.
 * Works without a repositry.
 */
public abstract class DefaultRestService<M extends Base> {
  private static final Logger logger = LoggerFactory.getLogger(DefaultRestService.class);
  private Repository<M> repository = null;
  private Class repositoryClass;
  private Class modelClass;
  public static final MetricRegistry metrics = new MetricRegistry();

  @Inject
  private Service service;

  public Service service() {
    return service;
  }

  @SuppressWarnings("unchecked")
  public Repository<M> repository() {
    if(repository == null) {
      try {
        repository = (Repository<M>) service().repository(getModelClass());
      } catch(ClassNotFoundException e) {
        logger.error("class for service {}", this.getClass().getName());
      }
    }
    return repository;
  }

  private Class getModelClass() throws ClassNotFoundException {
    if(modelClass == null) {
      Type[] parameterizedTypes = ReflectionUtil.getParameterizedTypes(this);
      modelClass = ReflectionUtil.getClass(parameterizedTypes[0]);
    }
    return modelClass;
  }
}
