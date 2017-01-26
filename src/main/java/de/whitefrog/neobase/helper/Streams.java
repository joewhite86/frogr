package de.whitefrog.neobase.helper;

import de.whitefrog.neobase.collection.ResultIterator;
import de.whitefrog.neobase.model.Base;
import org.neo4j.graphdb.ResourceIterator;

import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {
  public static <T extends Base> Stream<T> get(ResultIterator<T> iterator) {
    return StreamSupport
      .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
      .onClose(iterator::close);
  }
  public static <T extends Base> Stream<T> get(ResourceIterator<T> iterator) {
    return StreamSupport
      .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
      .onClose(iterator::close);
  }
  
  public static <T extends Base> T single(Stream<T> stream) {
    Optional<T> optional = stream.findFirst();
    return optional.isPresent()? optional.get(): null;
  }
}
