package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Result;

import java.util.*;

public class ExecutionResultIterator<T extends Base> extends ResultIterator<T> {
  private final Service service;
  private final Result results;
  private final SearchParameter params;
  private Map<String, Object> next = null;

  public ExecutionResultIterator(Service service, Result results, SearchParameter params) {
    super(null, results);
    this.results = results;
    this.params = params;
    this.service = service;
  }
  @SuppressWarnings("unchecked")
  public ExecutionResultIterator(Repository<T> repository, Result results, SearchParameter params) {
    super(repository, results);
    this.params = params;
    this.results = results;
    this.service = repository.service();
  }

  private Repository repository(Node node) {
    return service.repository((String) node.getProperty(Base.Type));
  }

  @Override
  public boolean hasNext() {
    return next != null || results.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T next() {
    Map<String, Object> result = next != null? next: results.next();
    String identifier = CollectionUtils.isEmpty(params.returns())? 
      repository().queryIdentifier(): params.returns().get(0);
    Node node = (Node) result.get(identifier);
    T model = (T) (repository() != null? repository(): repository(node))
      .createModel(node, params.fieldList());
    if(result.size() > 1) {
      Map<String, List<Base>> map = new HashMap<>();
      boolean nextFound = false;
      while(!nextFound && results.hasNext()) {
        next = results.next();
        Node nextNode = (Node) next.get(identifier);
        if(node.equals(nextNode)) {
          for(String fieldName: next.keySet()) {
            if(fieldName.equals(identifier)) continue;
            PropertyContainer item = (PropertyContainer) next.get(fieldName);
            if(!map.containsKey(fieldName)) map.put(fieldName, new ArrayList<>());
            FieldList fields = params.fieldList().containsField(fieldName)?
              params.fieldList().get(fieldName).subFields(): FieldList.parseFields(Base.AllFields);
            Base base = Persistence.get(item, fields);
            map.get(fieldName).add(base);
          }
        }
        else {
          nextFound = true;
        }
      }
      for(String fieldName: map.keySet()) {
        try {
          FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(model.getClass(), fieldName);
          descriptor.field().set(model, Set.class.isAssignableFrom(descriptor.field().getType())?
            new HashSet(map.get(fieldName)): map.get(fieldName));
        } catch(IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      if(!nextFound) next = null;
    }
    return model;
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
