Application
===========

In our main ``Application`` class, we can implement additional features, such as a custom ``Service`` implementation, metrics,
or authorization. See the chapter on :doc:`Authorization and Security <authorization>` for further information.

.. literalinclude:: ../../../examples/custom-service/src/main/java/de/whitefrog/frogr/example/customservice/MyApplication.java
  :language: java
  :lines: 17-

Service Injector
----------------

We can also write our own ``ServiceInjector``, in case we want to override the base ``Service``.

.. literalinclude:: ../../../examples/custom-service/src/main/java/de/whitefrog/frogr/example/customservice/rest/request/MyServiceInjector.java
  :language: java
  :lines: 7-

In that case, ``Service`` is our own implementation and should extend ``de.whitefrog.frogr.Service``.

Service
-------

For instance if we want to provide a common configuration accessible from any :doc:`Repository <repositories>` or :doc:`Service <services>`:

.. literalinclude:: ../../../examples/custom-service/src/main/java/de/whitefrog/frogr/example/customservice/MyService.java
  :language: java
  :lines: 10-