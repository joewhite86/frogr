package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.repository.Repository;
import de.whitefrog.neobase.Service;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionResultIterator<T extends de.whitefrog.neobase.model.Model> extends ResultIterator<T> {
  private Service service;
  private ResourceIterator<Node> results = null;
  private FieldList fields = new FieldList();

  @SuppressWarnings("unchecked")
  public ExecutionResultIterator(Repository<T> repository, Result results, String identifier) {
    super(repository, results.columnAs(identifier));
    this.results = (ResourceIterator<Node>) baseIterator();
  }

  @SuppressWarnings("unchecked")
  public ExecutionResultIterator(Service service, Result results, String identifier) {
    super(null, results.columnAs(identifier));
    this.service = service;
    this.results = (ResourceIterator<Node>) baseIterator();
  }

  private Repository repository(Node node) {
    return service.repository((String) node.getProperty(Base.Type));
  }

  public ExecutionResultIterator<T> fields(String... fields) {
    return fields(Arrays.asList(fields));
  }

  public ExecutionResultIterator<T> fields(List<String> fields) {
    FieldList fieldList = fields.stream()
      .map(QueryField::new)
      .collect(Collectors.toCollection(FieldList::new));
    return fields(fieldList);
  }

  public ExecutionResultIterator<T> fields(FieldList fields) {
    this.fields = fields;
    return this;
  }

  @Override
  public boolean hasNext() {
    return results.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T next() {
    Node node = results.next();
    return (T) (repository() != null? repository(): repository(node))
      .createModel(node, fields);
  }

  @Override
  public void remove() {
    results.remove();
  }

  @Override
  public void close() {
    if(results != null) results.close();
  }
}
