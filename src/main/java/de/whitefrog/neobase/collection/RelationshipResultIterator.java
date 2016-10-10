package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.persistence.Persistence;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RelationshipResultIterator<T extends Base> extends ResultIterator<T> {
  private ResourceIterator<Relationship> results = null;
  private FieldList fields = new FieldList();

  @SuppressWarnings("unchecked")
  public RelationshipResultIterator(Result results, String identifier) {
    super(null, results.columnAs(identifier));
    this.results = (ResourceIterator<Relationship>) baseIterator();
  }

  public RelationshipResultIterator<T> fields(String... fields) {
    return fields(Arrays.asList(fields));
  }

  public RelationshipResultIterator<T> fields(List<String> fields) {
    FieldList fieldList = fields.stream()
      .map(QueryField::new)
      .collect(Collectors.toCollection(FieldList::new));
    return fields(fieldList);
  }

  public RelationshipResultIterator<T> fields(FieldList fields) {
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
    Relationship relationship = results.next();
    return Persistence.get(relationship, fields);
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
