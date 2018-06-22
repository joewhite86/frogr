package de.whitefrog.frogr.example.basic.repository;

import de.whitefrog.frogr.example.basic.model.MarriedWith;
import de.whitefrog.frogr.example.basic.model.Person;
import de.whitefrog.frogr.model.FieldList;
import de.whitefrog.frogr.model.Filter;
import de.whitefrog.frogr.repository.BaseModelRepository;

import java.util.Arrays;
import java.util.List;

public class PersonRepository extends BaseModelRepository<Person> {
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
    jerry.setChildren(Arrays.asList(morty, summer));
    save(rick, beth, jerry, morty, summer);
    MarriedWith marriedWith = new MarriedWith(jerry, beth);
    marriedWith.setYears(10L);
    service().repository(MarriedWith.class).save(marriedWith);
  }

  public void searchExamples() {
    String uuid1 = "123", uuid2 = "345";
    // Filter results by uuids and return the name and the person married with the found person.
    List<Person> results = search()
      .uuids(uuid1, uuid2)
      .fields(Person.Name, Person.MarriedWith)
      .list();

    // Get a count of persons, where on of its parents name is "Jerry Smith".
    long count = search()
      .filter(new Filter.Equals("parents.name", "Jerry Smith"))
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
  }
}
