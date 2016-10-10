package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.helpers.collection.Iterators;

import java.io.Closeable;
import java.util.Iterator;
import java.util.List;

public abstract class ResultIterator<T extends Base> implements Iterator<T>, Closeable {
  private Repository repository;
  private Iterator iterator;

  public ResultIterator(Repository repository, Iterator iterator) {
    this.iterator = iterator;
    this.repository = repository;
  }

  public Repository repository() {
    return repository;
  }

  public Iterator baseIterator() {
    return iterator;
  }

  public List<T> asList() {
    return Iterators.asList(this);
  }
  
  public T single() {
    return (T) Iterators.single(this);
  }

  public void close() {

  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("not supported");
  }
}
