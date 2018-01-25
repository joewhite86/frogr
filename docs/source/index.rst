Froggy - Neo4j powered restful drop-in solution
===============================================

Java:

.. code-block:: java

  PersonRepository repository = service().repository(Person.class);
  List<Person> persons = personRepository.search()
    .query("*Smith"))
    .fields("name", "children.name")
    .list()

REST:

.. code-block:: rest
  
  GET localhost:8282/persons?q=*Smith&fields=name,children.name

.. code-block:: json

  {
    "success": true,
    "data": [
      {
        "uuid": "99eaeddc012811e883fda165f97f8411",
        "type": "Person",
        "name": "Beth Smith",
        "children": [
          {
            "uuid": "99eb630e012811e883fd97343769a63e",
            "type": "Person",
            "name": "Morty Smith"
          },
          {
            "uuid": "99ebb12f012811e883fd2583ad59c919",
            "type": "Person",
            "name": "Summer Smith"
          }
        ]
      },
      {
        "uuid": "99eb3bfd012811e883fdd551cdefd5ed",
        "type": "Person",
        "name": "Jerry Smith",
        "children": [
          {
            "uuid": "99eb630e012811e883fd97343769a63e",
            "type": "Person",
            "name": "Morty Smith"
          },
          {
            "uuid": "99ebb12f012811e883fd2583ad59c919",
            "type": "Person",
            "name": "Summer Smith"
          }
        ]
      },
      {
        "uuid": "99eb630e012811e883fd97343769a63e",
        "type": "Person",
        "name": "Morty Smith",
        "children": []
      },
      {
        "uuid": "99ebb12f012811e883fd2583ad59c919",
        "type": "Person",
        "name": "Summer Smith",
        "children": []
      }
    ]
  }


.. toctree::
   :maxdepth: 2
   :caption: Contents:

   guide
   documentation
   license
   help
