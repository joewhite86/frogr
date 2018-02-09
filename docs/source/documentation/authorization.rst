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
      <version>0.1.2-SNAPSHOT</version>
  </dependency>

User Model
^^^^^^^^^^

The User model has to extend ``BaseUser`` and defines our user, which can be passed 
in :doc:`Service <services>` methods using the ``@Auth`` annotation.

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/model/User.kt
  :language: kotlin
  :lines: 9-

As you can see the annotation ``@JsonView(Views.Secure::class)`` is used on the ``friends`` field.
These views can be used on ``Service`` methods too, and describe what can be seen by the user.
The default is ``Views.Public::class``, so any field annotated with that ``@JsonView`` is visible to everyone.
Fields without ``@JsonView`` annotation are always visible.

``BaseUser`` provides some commonly used fields describing a user in an authentication environment:

.. literalinclude:: ../../../auth/src/main/java/de/whitefrog/frogr/auth/model/BaseUser.kt
  :language: kotlin
  :lines: 13-

You can write your own User class, but then you'll have to create your own oAuth implementation.

**Warning:** Be cautious with ``Views.Secure`` on ``Service`` methods, as it could reveal sensitive data. 
So it's best to have custom methods like ``findFriendsOfFriends`` for example to get all friends of the users friends.

User Repository
^^^^^^^^^^^^^^^

Next, we'll have to define a repository for our users, extending ``BaseUserRepository``:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/repository/UserRepository.java
  :language: java
  :lines: 10-

The extended class ``BaseUserRepository`` provides some basic functionality and security.

``register(user)``
  Registration of a new user, passwords will be encrypted by default.
``login(login, password)``
  Login method, encrypts the password automatically for you.
``validateModel(context)``
  Overridden to ensure a password and a role is set on new users.

Application
^^^^^^^^^^^

In our applications ``run`` method, we need to set up some authentication configurations:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/MyApplication.java
  :language: java
  :lines: 15-

Inside the ``run`` method, we set up ``RolesAllowedDynamicFeature``, which activates the previously used ``@RolesAllowed``
annotation. We also set up ``AuthValueFactoryProvider.Binder`` which activates the later described ``@Auth`` injection annotation and
``AuthDynamicFeature`` which activates the actual oAuth authentication.

Services
^^^^^^^^

Here's a simple service, that can only be called when the user is authenticated. 
The user will be passed as argument to the method:

.. literalinclude:: ../../../examples/oauth/src/main/java/de/whitefrog/frogr/auth/example/rest/Persons.java
  :language: java
  :lines: 15-

See how the ``@RolesAllowed(Role.User)`` annotation is used, to only allow this method to registered users.
You can always extend the Role class and use your own roles. Predefined roles are ``Admin``, ``User`` and ``Public``.

The first (and only) parameter on ``findMorty`` is annotated with ``@Auth`` and has the type of our ``User`` class created before.
This will inject the currently authenticated user and also tells the application that this method is only allowed for authenticated users.

The extended class ``AuthCRUDService`` provides some convenient methods for authentication and sets some default ``@RolesAllowed`` annotations
on the basic CRUD methods. All predefined methods are only allowed for registered and authenticated users.

``authorize``
  Override to implement your access rules for specific models. Is used by default in create and update methods.
``authorizeDelete``
  Override to implement your rules to who can delete specific models. Is used by default in delete method.
