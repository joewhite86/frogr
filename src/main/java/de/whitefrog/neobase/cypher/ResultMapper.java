package de.whitefrog.neobase.cypher;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Entity;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.PropertyContainer;

import java.util.*;
import java.util.function.Function;

public class ResultMapper<T extends Base> implements Function<Map<String, Object>, T> {
  private final SearchParameter params;
  private final Repository<T> repository;
  
  public ResultMapper(Repository<T> repository, SearchParameter params) {
    this.repository = repository;
    this.params = params;
  }
  @Override
  public T apply(Map<String, Object> result) {
    String identifier = CollectionUtils.isEmpty(params.returns())?
      repository.queryIdentifier(): params.returns().get(0);
    PropertyContainer node = (PropertyContainer) result.get(identifier);
    T model = repository.createModel(node, params.fieldList());

    // when some fields are fetched in query result already they will be added to the model
    if(result.size() > 1) {
      // TODO
    }
    return model;
  }
}
