package de.whitefrog.froggy.rest.service;

import com.codahale.metrics.MetricRegistry;
import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.helper.ReflectionUtil;
import de.whitefrog.froggy.model.Base;
import de.whitefrog.froggy.model.rest.SearchParameter;
import de.whitefrog.froggy.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.List;

public class DefaultRestService<M extends Base> {
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

    protected void sort(List<M> list, SearchParameter params) {
        repository().sort(list, params.orderBy());
    }
}
