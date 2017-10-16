package de.whitefrog.froggy.helper;

import de.whitefrog.froggy.model.Base;

import java.util.Optional;
import java.util.stream.Stream;

public class Streams {  
  public static <T extends Base> T single(Stream<T> stream) {
    Optional<T> optional = stream.findFirst();
    return optional.isPresent()? optional.get(): null;
  }
}
