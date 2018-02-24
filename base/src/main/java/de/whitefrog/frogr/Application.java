package de.whitefrog.frogr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import de.whitefrog.frogr.health.GraphHealthCheck;
import de.whitefrog.frogr.rest.request.SearchParameterResolver;
import de.whitefrog.frogr.rest.request.ServiceInjector;
import de.whitefrog.frogr.rest.response.ExceptionMapper;
import de.whitefrog.frogr.rest.response.WrappingWriterInterceptor;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * The base REST application entry point. Starts up a service instance and some settings required for REST.
 */
public abstract class Application<C extends io.dropwizard.Configuration> extends io.dropwizard.Application<C> {
  private static final Logger logger = LoggerFactory.getLogger(Application.class);
  private static final String AllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";
  private static final String AllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin,Authorization";

  private List<String> packages = new ArrayList<>();
  private ServiceInjector serviceInjector;

  public Service service() {
    return serviceInjector().provide();
  }
  public ServiceInjector serviceInjector() {
    if(serviceInjector == null) {
      serviceInjector = new ServiceInjector();
    }
    return serviceInjector;
  }

  @Override
  public void initialize(Bootstrap<C> bootstrap) {
    bootstrap.addBundle(new MultiPartBundle());
    super.initialize(bootstrap);
  }
  
  @Override
  public void run(C configuration, Environment environment) throws Exception {
    environment.jersey().register(ExceptionMapper.class);
    environment.jersey().register(SearchParameterResolver.class);

    environment.jersey().packages(packages.toArray(new String[packages.size()]));

//    final FilterRegistration.Dynamic cors =
//      environment.servlets().addFilter("CORS", CrossOriginFilter.class);
//    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
//    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
//    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, AllowedHeaders);
//    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, AllowedMethods);
//    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, AllowedMethods);
//    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    environment.healthChecks().register("graph", new GraphHealthCheck(service()));

    environment.getObjectMapper().enable(MapperFeature.USE_ANNOTATIONS);
    environment.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    environment.jersey().register(WrappingWriterInterceptor.class);

    environment.jersey().register(new AbstractBinder() {
      @Override
      protected void configure() {
        bindFactory(serviceInjector()).to(Service.class);
      }
    });

    // print the port in log
    if(logger.isInfoEnabled()) {
      if(configuration.getServerFactory() instanceof DefaultServerFactory) {
        DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();
        for(ConnectorFactory connector : serverFactory.getApplicationConnectors()) {
          if(connector.getClass().isAssignableFrom(HttpConnectorFactory.class)) {
            logger.info("Service available at: http://localhost:{}", ((HttpConnectorFactory) connector).getPort());
            break;
          }
        }
      }
      else {
        SimpleServerFactory serverFactory = (SimpleServerFactory) configuration.getServerFactory();
        HttpConnectorFactory connector = (HttpConnectorFactory) serverFactory.getConnector();
        if(connector.getClass().isAssignableFrom(HttpConnectorFactory.class)) {
          logger.info("Service available at: http://localhost:{}", connector.getPort());
        }
      }
    }
  }

  public void register(String... packages) {
    if(packages.length == 1 && packages[0].contains(";")) {
      packages = packages[0].split(";");
    }

    this.packages.addAll(Arrays.asList(packages));
  }
  
  public List<String> registry() {
    return packages;
  }
  
  public void shutdown() {
    if(service() != null) service().shutdown();
    else logger.error("service not initialized");
  }
}
