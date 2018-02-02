Searching
=========

Java
----

In Java code there's a easy to use method in each repository.
Here are some examples:

.. literalinclude:: ../../../base/examples/basic/src/main/java/de/whitefrog/frogr/example/repository/PersonRepository.java
  :language: java
  :lines: 34-56
  :dedent: 4

REST
----

Over HTTP you would normally use a ``CRUDService``, that provides the neccessary methods, but we can of course write our own ones.
Here are some examples, that would return the same results as the Java queries above:

.. code-block:: rest

  // Filter results by uuids and return the name and the person married with the found person.
  http://localhost:8282/persons?uuids=e4633739050611e887032b418598e63f,e4635e4a050611e88703efbc809ff2fd&fields=name,marriedWith

.. code-block:: json
  
  {
    "success": true,
    "data": [
      {
        "uuid": "e4633739050611e887032b418598e63f",
        "type": "Person",
        "name": "Beth Smith",
        "marriedWith": {
          "uuid": "e4635e4a050611e88703efbc809ff2fd",
          "type": "Person"
        }
      },
      {
        "uuid": "e4635e4a050611e88703efbc809ff2fd",
        "type": "Person",
        "name": "Jerry Smith",
        "marriedWith": {
          "uuid": "e4633739050611e887032b418598e63f",
          "type": "Person"
        }
      }
    ]
  }

.. code-block:: rest

  // Get a count of persons, where on of its parents name is "Jerry Smith".
  http://localhost:8282/persons?filter=parents.name:=Jerry%20Smith&count

.. code-block:: json
  
  {
    "success": true,
    "total": 2,
    "data": [
      {
        "uuid": "e463d37c050611e887034f42b099b0cd",
        "type": "Person"
      },
      {
        "uuid": "e463ac6b050611e887038de1cbd926c1",
        "type": "Person"
      }
    ]
  }

.. code-block:: rest

  // Get a paged result of all persons, with a page size of 10, ordered by the name property.
  http://localhost:8282/persons?limit=10&page=1&order=name&fields=name

.. code-block:: json

  {
    "success": true,
    "data": [
      {
        "uuid": "e4633739050611e887032b418598e63f",
        "type": "Person",
        "name": "Beth Smith"
      },
      {
        "uuid": "e4635e4a050611e88703efbc809ff2fd",
        "type": "Person",
        "name": "Jerry Smith"
      },
      {
        "uuid": "e463ac6b050611e887038de1cbd926c1",
        "type": "Person",
        "name": "Morty Smith"
      },
      {
        "uuid": "e4607818050611e8870361190053d169",
        "type": "Person",
        "name": "Rick Sanchez"
      },
      {
        "uuid": "e463d37c050611e887034f42b099b0cd",
        "type": "Person",
        "name": "Summer Smith"
      }
    ]
  }

.. code-block:: rest

  http://localhost:8282/persons?filter=name:=Beth%20Smith&fields=name,children.name

.. code-block:: json

  {
    "success": true,
    "data": [
      {
        "uuid": "e4633739050611e887032b418598e63f",
        "type": "Person",
        "name": "Beth Smith",
        "children": [
          {
            "uuid": "e463ac6b050611e887038de1cbd926c1",
            "type": "Person",
            "name": "Morty Smith"
          },
          {
            "uuid": "e463d37c050611e887034f42b099b0cd",
            "type": "Person",
            "name": "Summer Smith"
          }
        ]
      }
    ]
  }

Usage in services
-----------------

If we want to write a method that takes its own search parameters, we can use the ``@SearchParam`` annotation along with a ``SearchParameter`` argument:

.. literalinclude:: ../../../base/examples/basic/src/main/java/de/whitefrog/frogr/example/rest/Persons.java
  :language: java
  :lines: 14-15,28-35

Parameters
----------

These are the possible querystring parameters, but you can find nearly identical methods in Java too.

``uuids``
  Comma-seperated list of uuids to search.
``query``
  Searches all indexed fields for a query string.
``count``
  Add a total value of found records, useful if the result is limited.
``start``
  Start returning results at a specific position, not required when ``page`` is set.
``limit``
  Limit the results.
``page``
  Page to return. Takes the limit parameter and sets the cursor to the needed position.
``filter``/``filters``
  Filter to apply. Filters start with the field name, followed by a ``:`` and the comparator. 
  Valid comparators are: 

| ``=`` Equal
| ``!`` Not equal
| ``<`` less than
| ``<=`` less or equal than
| ``>`` greater than
| ``>=`` greater or equal than
| ``({x}-{y})`` in a range between x and y
|

``fields``
  Comma-seperated list of fields to fetch. Can also fetch sub-fields of related models seperated by a ``.``, for example ``children.name`` would fetch all childrens and their names.
  Multiple sub-fields can be fetched inside curly braces, ``children.{name,age}`` would fetch all childrens and their names and ages.
``return``/``returns``
  Returns a related model instead of the service model. 
``order``/``orderBy``/``sort``
  Comma-seperated list of fields on which the results are sorted. ``-`` before the field sorts in descending, ``+`` in ascending direction. If bypassed ascending direction is used.