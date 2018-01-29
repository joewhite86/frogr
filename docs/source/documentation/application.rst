Application
===========

In our main ``Application`` class, we can implement additional features, such as a custom ``Service`` implementation, metrics,
or authorization. See the chapter on :doc:`Authorization and Security <authorization>` for further information.

.. code-block:: java

  public class MyApplication extends Application<Configuration> {
    private MyServiceInjector serviceInjector;

    public MyApplication() {
      // register the rest classes
      register("de.whitefrog.frogr.example.rest");
      // register repositories and models
      serviceInjector().service().register("de.whitefrog.frogr.example");
    }

    // override to pass our own ServiceInjector implementation
    @Override
    public ServiceInjector serviceInjector() {
      if(serviceInjector == null) {
        serviceInjector = new MyServiceInjector();
      }
      return serviceInjector;
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
      super.run(configuration, environment);

      // bind the custom ServiceInjector to our Service implementation, described below
      environment.jersey().register(new AbstractBinder() {
        @Override
        protected void configure() {
          bindFactory(serviceInjector()).to(MyService.class);
        }
      });

      // register metrics
      environment.jersey().register(new InstrumentedResourceMethodApplicationListener(RestService.metrics));

      // add a console reporter for the metrics
      final ConsoleReporter reporter = ConsoleReporter.forRegistry(RestService.metrics)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
      // report every 30 minutes
      reporter.start(30, TimeUnit.MINUTES);

      // add a logger reporter for the metrics
      final Slf4jReporter slf4j = Slf4jReporter.forRegistry(RestService.metrics)
        .outputTo(LoggerFactory.getLogger("de.whitefrog.myband.metrics"))
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
      // report every 30 minutes
      slf4j.start(30, TimeUnit.MINUTES);
    }
    
    @Override
    public String getName() {
      return "example-rest";
    }

    public static void main(String[] args) throws Exception {
      new MyApplication().run("server", "config/example.yml");
    }
  }

Service Injector
----------------

We can also write our own ``ServiceInjector``, in case we want to override the base ``Service``.

.. code-block:: java

  public class MyServiceInjector extends ServiceInjector {
    private MyService service;
    
    public MyServiceInjector() {
      service = new MyService();
    } 

    @Override
    public Service service() {
      return service;
    }

    @Override
    public Service provide() {
      if(!service.isConnected()) service.connect();
      return service;
    }

    @Override
    public void dispose(de.whitefrog.frogr.Service service) {
      service.shutdown();
    }
  }

In that case, ``Service`` is our own implementation and should extend ``de.whitefrog.frogr.Service``.

Service
-------

For instance if we want to provide a common configuration accessible from any :doc:`Repository <repositories>` or :doc:`Service <services>`:

.. literalinclude:: ../../../base/examples/custom-service/src/main/java/de/whitefrog/frogr/example/MyService.java
  :language: java
  :lines: 10-