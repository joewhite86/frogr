package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

public class NodeIterator<T extends Model> extends ResultIterator<T> {
  private ResourceIterator<Node> iterator = null;
  private FieldList fields;
  private int skip = 0;
  private int skipped = 0;

  public NodeIterator(Repository<T> repository, ResourceIterator<Node> iterator) {
    super(repository, iterator);
    this.iterator = iterator;
  }

  public NodeIterator(Repository<T> repository, ResourceIterator<Node> iterator, SearchParameter params) {
    super(repository, iterator);
    this.iterator = iterator;
    this.fields = FieldList.parseFields(params.fields());
    this.skip = (params.page() - 1) * params.limit();
  }

  public NodeIterator(Repository<T> repository, ResourceIterator<Node> iterator, int skip) {
    this(repository, iterator);
    this.skip = skip;
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
    Node node = iterator.next();
    return (T) repository().createModel(node, fields);
  }

  @Override
  public void remove() {
    iterator.remove();
  }
}
