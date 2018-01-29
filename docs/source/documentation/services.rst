Services
========

Services define the REST entry points of our application. They should only have minimal logic and communicate with
the repositories instead.

Services can extend the CRUDService class, which provides basic CRUD methods for creating, reading, updating and deleting models.
They also can extend the RestService class, which only provides some minimal convenience methods.
But we can also create a service from scratch.

Here's a basic example of a service, used for the Person models:

.. code-block:: java

  @Path("persons")
  public class Persons extends CRUDService<PersonRepository, Person> {
    @GET
    @Path("init")
    public void init() {
      // insert some data
      try(Transaction tx = service().beginTx()) {
        if(repository().search().count() == 0) {
          repository().init();
          tx.success();
        }
      }
    }
  }

And here's the repository implementation:

.. code-block:: java

  public class PersonRepository extends BaseModelRepository<Person> {
    public PersonRepository(Service service) {
      super(service);
    }
    public void init() {
      Person rick = new Person("Rick Sanchez");
      Person beth = new Person("Beth Smith");
      Person jerry = new Person("Jerry Smith");
      Person morty = new Person("Morty Smith");
      Person summer = new Person("Summer Smith");
      // we need to save the people first, before we can create relationships
      save(rick, beth, jerry, morty, summer);

      rick.setChildren(Arrays.asList(beth));
      beth.setChildren(Arrays.asList(morty, summer));
      beth.setMarriedWith(jerry);
      jerry.setChildren(Arrays.asList(morty, summer));
      jerry.setMarriedWith(beth);
      save(rick, beth, jerry, morty, summer);
    }
  }

This would create these REST paths:

::

  POST    /persons (de.whitefrog.frogr.example.rest.Persons)
  GET     /persons (de.whitefrog.frogr.example.rest.Persons)
  GET     /persons/{uuid: [a-zA-Z0-9]+} (de.whitefrog.frogr.example.rest.Persons)
  POST    /persons/search (de.whitefrog.frogr.example.rest.Persons)
  PUT     /persons (de.whitefrog.frogr.example.rest.Persons)
  DELETE  /persons/{uuid: [a-zA-Z0-9]+} (de.whitefrog.frogr.example.rest.Persons)
  GET     /persons/init (de.whitefrog.frogr.example.rest.Persons)

The ``POST`` and ``PUT`` methods both take a json object, representing the model to create or update.

The ``GET`` method takes url parameters used for a search operation. See :doc:`Search <search>` for further details.
Additionally there's also a ``GET /{uuid}`` method for convenience, which searches for a particular person.

We can also use ``POST /search`` path, which takes the search parameters as a json object, but I strongly recommend using GET for that purpose, as only GET can be cached correctly.

``DELETE`` has the UUID inside its path and will delete the model with that UUID.

We can also see our previously implemented ``/init`` path configured.

We use primarily UUIDs to reference models, not Ids as they can be reused by the underlying Neo4j_ database.

.. _Neo4j: http://neo4j.com