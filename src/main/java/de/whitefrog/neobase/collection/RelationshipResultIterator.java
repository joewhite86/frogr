package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.Persistence;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelationshipResultIterator<T extends Base> extends ResultIterator<T> {
  private final ResourceIterator<Relationship> results;
  private final SearchParameter params;

  @SuppressWarnings("unchecked")
  public RelationshipResultIterator(Result results, SearchParameter params) {
    super(null, results.columnAs(params.returns().get(0)));
    this.results = (ResourceIterator<Relationship>) baseIterator();
    this.params = params; 
  }

  @Override
  public boolean hasNext() {
    return results.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T next() {
    Relationship relationship = results.next();
    return Persistence.get(relationship, params.fieldList());
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
