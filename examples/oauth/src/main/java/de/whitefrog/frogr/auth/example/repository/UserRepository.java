package de.whitefrog.frogr.auth.example.repository;

import de.whitefrog.frogr.auth.example.model.Person;
import de.whitefrog.frogr.auth.example.model.User;
import de.whitefrog.frogr.auth.model.Role;
import de.whitefrog.frogr.auth.repository.BaseUserRepository;

import java.util.Arrays;

public class UserRepository extends BaseUserRepository<User> {
  public String init() {
    String token;
    PersonRepository persons = service().repository(Person.class);
    if(search().count() == 0) {
      Person rick = new Person("Rick Sanchez");
      Person beth = new Person("Beth Smith");
      Person jerry = new Person("Jerry Smith");
      Person morty = new Person("Morty Smith");
      Person summer = new Person("Summer Smith");
      // we need to save the people first, before we can create relationships
      persons.save(rick, beth, jerry, morty, summer);

      rick.setChildren(Arrays.asList(beth));
      beth.setChildren(Arrays.asList(morty, summer));
      beth.setMarriedWith(jerry);
      jerry.setChildren(Arrays.asList(morty, summer));
      jerry.setMarriedWith(beth);
      persons.save(rick, beth, jerry, morty, summer);

      User user = new User();
      user.setLogin("justin_roiland");
      user.setPassword("rickandmorty");
      user.setRole(Role.Admin);
      save(user);
      
      // login to create and print an access token - for test purposes only
      user = login("justin_roiland", "rickandmorty");
      token = "Access Token: " + user.getAccessToken();
      System.out.println("User created. Authorization: Bearer " + user.getAccessToken());
    } else {
      User user = login("justin_roiland", "rickandmorty");
      token = "Access Token: " + user.getAccessToken();
      System.out.println("Authorization: Bearer " + user.getAccessToken());
    }
    return token;
  }
}
