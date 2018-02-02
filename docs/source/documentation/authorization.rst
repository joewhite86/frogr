Authorization and Security
==========================

Setup
-----

Maven
^^^^^

.. code-block:: xml

  <dependency>
      <groupId>de.whitefrog</groupId>
      <artifactId>frogr-auth</artifactId>
      <version>0.1.1-SNAPSHOT</version>
  </dependency>

User Model
^^^^^^^^^^

The User model has to extend ``BaseUser`` and defines our user, which can be passed 
in :doc:`Service <services>` methods using the ``@Auth`` annotation.

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/model/User.kt
  :language: kotlin
  :lines: 5

User Repository
^^^^^^^^^^^^^^^

Next, we'll have to define a repository for our users, extending ``BaseUserRepository``:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/repository/UserRepository.java
  :language: java
  :lines: 10-

Application
^^^^^^^^^^^

In our applications ``run`` method, we need to set up some authentication configurations:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/MyApplication.java
  :language: java
  :lines: 15-

Services
^^^^^^^^

Here's a simple service, that can only be called when the user is authenticated. 
The user will be passed as argument to the method:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/rest/Persons.java
  :language: java
  :lines: 15-