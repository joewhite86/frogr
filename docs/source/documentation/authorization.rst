Authorization and Security
==========================

Setup
-----

Maven
^^^^^

.. literalinclude:: ../../../auth/example/pom.xml
  :language: xml
  :lines: 22-26 
  :dedent: 8

User Model
^^^^^^^^^^

The User model has to extend ``BaseUser`` and defines our user, which can be passed 
in :doc:`Service <services>` methods using the ``@Auth`` annotation.

.. literalinclude:: ../../../auth/example/src/main/kotlin/de/whitefrog/frogr/auth/example/model/User.kt
  :language: kotlin
  :lines: 5

User Repository
^^^^^^^^^^^^^^^

Next, we'll have to define a repository for our users, extending ``BaseUserRepository``:

.. literalinclude:: ../../../auth/example/src/main/java/de/whitefrog/frogr/auth/example/repository/UserRepository.java
  :language: java
  :lines: 10-

Application
^^^^^^^^^^^

In our applications ``run`` method, we need to set up some authentication configurations:

.. literalinclude:: ../../../auth/example/src/main/java/de/whitefrog/frogr/auth/example/MyApplication.java
  :language: java
  :lines: 15-

Services
^^^^^^^^

Here's a simple service, that can only be called when the user is authenticated. 
The user will be passed as argument to the method:

.. literalinclude:: ../../../auth/example/src/main/java/de/whitefrog/frogr/auth/example/rest/Persons.java
  :language: java
  :lines: 15-