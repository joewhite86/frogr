package de.whitefrog.frogr

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import de.whitefrog.frogr.health.GraphHealthCheck
import de.whitefrog.frogr.rest.request.SearchParameterResolver
import de.whitefrog.frogr.rest.request.ServiceInjector
import de.whitefrog.frogr.rest.response.ExceptionMapper
import de.whitefrog.frogr.rest.response.WrappingWriterInterceptor
import io.dropwizard.Configuration
import io.dropwizard.forms.MultiPartBundle
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.server.DefaultServerFactory
import io.dropwizard.server.SimpleServerFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.glassfish.hk2.utilities.binding.AbstractBinder
import org.slf4j.LoggerFactory
import java.util.*
import javax.servlet.DispatcherType

/**
 * The base REST application entry point. Starts up a service instance and some settings required for REST.
 */
abstract class Application<C : Configuration> : io.dropwizard.Application<C>() {
  companion object {
    private val logger = LoggerFactory.getLogger(Application::class.java)
    private const val AllowedMethods = "OPTIONS,GET,PUT,POST,DELETE,HEAD"
    private const val AllowedHeaders = "X-Requested-With,Content-Type,Accept,Origin,Authorization"
  }

  private val packages = ArrayList<String>()
  private var serviceInjector: ServiceInjector? = null

  fun service(): Service {
    return serviceInjector().provide()
  }

  open fun serviceInjector(): ServiceInjector {
    if (serviceInjector == null) {
      serviceInjector = ServiceInjector()
    }
    return serviceInjector!!
  }

  override fun initialize(bootstrap: Bootstrap<C>?) {
    bootstrap!!.addBundle(MultiPartBundle())
    super.initialize(bootstrap)
  }

  @Throws(Exception::class)
  override fun run(configuration: C, environment: Environment) {
    environment.jersey().register(ExceptionMapper::class.java)
    environment.jersey().register(SearchParameterResolver::class.java)

    environment.jersey().packages(*packages.toTypedArray())

    val cors = environment.servlets().addFilter("CORS", CrossOriginFilter::class.java)
    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*")
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*")
    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, AllowedHeaders)
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, AllowedMethods)
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, AllowedMethods)
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")

    environment.healthChecks().register("graph", GraphHealthCheck(service()))

    environment.objectMapper.enable(MapperFeature.USE_ANNOTATIONS)
    environment.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    environment.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    environment.jersey().register(WrappingWriterInterceptor::class.java)

    environment.jersey().register(object : AbstractBinder() {
      override fun configure() {
        bindFactory(serviceInjector()).to(Service::class.java)
      }
    })

    // print the port in log
    if (logger.isInfoEnabled) {
      if (configuration.serverFactory is DefaultServerFactory) {
        val serverFactory = configuration.serverFactory as DefaultServerFactory
        for (connector in serverFactory.applicationConnectors) {
          if (connector.javaClass.isAssignableFrom(HttpConnectorFactory::class.java)) {
            logger.info("Service available at: http://localhost:{}", (connector as HttpConnectorFactory).port)
            break
          }
        }
      } else {
        val serverFactory = configuration.serverFactory as SimpleServerFactory
        val connector = serverFactory.connector as HttpConnectorFactory
        if (connector.javaClass.isAssignableFrom(HttpConnectorFactory::class.java)) {
          logger.info("Service available at: http://localhost:{}", connector.port)
        }
      }
    }
  }

  fun register(vararg packages: String) {
    if (packages.size == 1 && packages[0].contains(";")) {
      this.packages.addAll(packages[0].split(";"))
    } else {
      this.packages.addAll(Arrays.asList(*packages))
    }
  }

  fun registry(): List<String> {
    return packages
  }

  fun shutdown() {
    if (service().isConnected)
      service().shutdown()
    else
      logger.error("service not initialized")
  }
}
