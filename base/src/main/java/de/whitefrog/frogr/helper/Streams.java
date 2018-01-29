package de.whitefrog.frogr.helper;

import de.whitefrog.frogr.model.Model;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Abstract helper class for streams.
 */
public class Streams {
  /**
   * Get a single model from a stream
   * @param stream Stream to use
   * @return The first model inside a stream
   */
  public static <T extends Model> T single(Stream<T> stream) {
    Optional<T> optional = stream.findFirst();
    return optional.isPresent()? optional.get(): null;
  }
}
