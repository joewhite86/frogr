package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.rest.FieldList;

public class FetchResultIterator<T extends Model> extends ResultIterator<T> {
  private final ResultIterator<T> iterator;
  private FieldList fields;

  public FetchResultIterator(ResultIterator<T> iterator) {
    super(iterator.repository(), iterator.baseIterator());
    this.iterator = iterator;
  }

  @Override
  public void close() {
    iterator.close();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T next() {
    T model = iterator.next();
    if(model != null) {
      if(fields != null) repository().fetch(model, fields);
      else repository().fetch(model);
    }
    return model;
  }
}
