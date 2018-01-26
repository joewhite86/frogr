Repositories
============

Repositories are used to communicate with the underlying database. 
We won't need to create a repository for each :doc:`model <models>`. 
There's a default implementation, that will be used if no appropriate repository was found which provides basic functionality.

The naming of the repository is important, so that Froggy can find it.
Names should start with the :doc:`model <models>` name and end with "Repository" (case-sensitive) and it should extend ``BaseModelRepository`` or ``BaseRelationshipRepository``.

If, for example, we want a repository for the ``Person`` model, we would create a repository called ``PersonRepository``:

.. code-block:: java

  public class PersonRepository extends BaseModelRepository<Person> {
    public PersonRepository(Service service) {
      super(service);
    }

    public Person uselessExampleMethod(Person p) {
      return p;
    }
  }

We can access our repository easily by calling the ``.repository(..)`` method of the ``Service`` instance.
The method takes either the :doc:`model <models>` class or the name as string.
There's access to it in any REST service class and inside any repository:

.. code-block:: java

  @Path("persons")
  public class Persons extends CRUDService<PersonRepository, Person> {
    @GET
    public Country getCountry(@QueryParam("uuid") String personUuid) {
      return service().repository(Country.class)
        .search()
        .filter(new Filter.Equals("people.uuid", personUuid))
        .single();
    }
  }
