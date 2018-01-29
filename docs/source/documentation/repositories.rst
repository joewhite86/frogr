Repositories
============

Repositories are used to communicate with the underlying database. 
We won't need to create a repository for each :doc:`model <models>`. 
There's a default implementation, that will be used if no appropriate repository was found which provides basic functionality.

The naming of the repository is important, so that Frogr can find it.
Names should start with the :doc:`model <models>` name and end with "Repository" (case-sensitive) and it should extend ``BaseModelRepository`` or ``BaseRelationshipRepository``.

If, for example, we want a repository for the ``Person`` model, we would create a repository called ``PersonRepository``:

.. literalinclude:: ../../../base/examples/basic/src/main/java/de/whitefrog/frogr/example/repository/PersonRepository.java
  :language: java
  :lines: 11-27,56

We can access our repository easily by calling the ``.repository(..)`` method of the ``Service`` instance.
The method takes either the :doc:`model <models>` class or the name as string.
There's access to it in any REST service class and inside any repository:

.. literalinclude:: ../../../base/examples/basic/src/main/java/de/whitefrog/frogr/example/rest/Persons.java
  :language: java
  :lines: 11-