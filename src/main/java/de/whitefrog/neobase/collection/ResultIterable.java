package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Model;

public class ResultIterable<T extends Model> implements Iterable<T> {
  private ResultIterator<T> iterator;

  public ResultIterable(ResultIterator<T> iterator) {
    this.iterator = iterator;
  }

  @Override
  public ResultIterator<T> iterator() {
    return iterator;
  }

  public ResultIterable<T> fetch() {
    iterator = new FetchResultIterator<>(iterator);
    return this;
  }
}
