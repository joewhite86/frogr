package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;

public class DefaultResultIterator<T extends Base> extends ResultIterator<T> {
  private ResourceIterator<? extends PropertyContainer> iterator = null;
  private FieldList fields;
  private int skip = 0;
  private int skipped = 0;

  public DefaultResultIterator(Repository<T> repository, ResourceIterator<? extends PropertyContainer> iterator) {
    super(repository, iterator);
    this.iterator = iterator;
  }

  public DefaultResultIterator(Repository<T> repository, ResourceIterator<? extends PropertyContainer> iterator, SearchParameter params) {
    super(repository, iterator);
    this.iterator = iterator;
    this.fields = FieldList.parseFields(params.fields());
    this.skip = (params.page() - 1) * params.limit();
  }

  @Override
  public void close() {
    iterator.close();
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = iterator.hasNext();

    if(!hasNext) {
      close();
    }

    return hasNext;
  }

  @Override
  public T next() {
    if(skipped != skip && skipped++ < skip && hasNext()) {
      System.out.println("skip " + skip + " skipped " + skipped);
      return next();
    }
    PropertyContainer node = iterator.next();
    return (T) repository().createModel(node, fields);
  }

  @Override
  public void remove() {
    iterator.remove();
  }
}
