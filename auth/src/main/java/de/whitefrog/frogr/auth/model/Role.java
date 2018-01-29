package de.whitefrog.frogr.auth.model;

import java.util.*;

public class Role {
  public static final String Admin = "Admin";
  public static final String User = "User";
  public static final String Public = "Public";

  private static final Map<String, List<String>> includes = createIncludes();

  private static Map<String, List<String>> createIncludes() {
    Map<String, List<String>> includes = new HashMap<>(2);

    includes.put(Admin, Arrays.asList(User, Public));
    includes.put(User, Collections.singletonList(Public));

    return includes;
  }

  public static boolean inRole(String userRole, String role) {
    return userRole.equals(role) || includes.containsKey(userRole) && includes.get(userRole).contains(role);
  }
}
