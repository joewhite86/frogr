Searching
=========

Java
----

In Java code there's a easy to use method in each repository.
Here are some examples:

.. code-block:: java

  // Filter results by uuids and return the name and the person married with the found person.
  List<Person> results = search()
    .uuids(uuid1, uuid2)
    .fields(Person.Name, Person.MarriedWith)
    .list();

  // Get a count of persons, where on of its parents name is "Jerry Smith".
  long count = search()
    .filter("parents.name", Filter.Equals("Jerry Smith"))
    .count();

  // Get a paged result of all persons, with a page size of 10, ordered by the name property.
  List<Person> page = search()
    .limit(10)
    .page(1)
    .orderBy(Person.Name)
    .fields(Person.Name)
    .list();

  // Get a single person and its children with their names.
  Person beth = search()
    .filter(Person.Name, "Beth Smith")
    .fields(FieldList.parseFields("name,children.name"))
    .single();

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