package de.whitefrog.neobase;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import de.whitefrog.neobase.rest.request.SearchParameterResolver;
import de.whitefrog.neobase.rest.request.ServiceInjector;
import de.whitefrog.neobase.rest.response.ExceptionMapper;
import de.whitefrog.neobase.rest.response.WrappingWriterInterceptor;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@Path("/")
@Singleton
public abstract class Application<C extends io.dropwizard.Configuration> extends io.dropwizard.Application<C> {
  private static final String AllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD";
  private static final String AllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin,Authorization";

  private List<String> packages = new ArrayList<>();
  private ServiceInjector serviceInjector;

  public Application() {
  }
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

    final FilterRegistration.Dynamic cors =
      environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, AllowedHeaders);
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, AllowedMethods);
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, AllowedMethods);
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

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
  }

  public void register(String... packages) {
    if(packages.length == 1 && packages[0].contains(";")) {
      packages = packages[0].split(";");
    }

    this.packages.addAll(Arrays.asList(packages));
  }

  public void shutdown() {
    if(service() != null) service().shutdown();
    else LoggerFactory.getLogger(Application.class).error("service not initialized");
  }
}
