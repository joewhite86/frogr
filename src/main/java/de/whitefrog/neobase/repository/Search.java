package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.collection.ExecutionResultIterator;
import de.whitefrog.neobase.collection.RelationshipResultIterator;
import de.whitefrog.neobase.exception.RepositoryInstantiationException;
import de.whitefrog.neobase.helper.StreamUtils;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.Persistence;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Search {
  private final Service service;
  private final Repository<? extends Model> repository;
  private SearchParameter params;
  
  public Search(Repository<? extends Model> repository) {
    this.repository = repository;
    this.service = repository.service();
    this.params = new SearchParameter();
  }

  private Stream<? extends Base> search(SearchParameter params) {
    if(CollectionUtils.isEmpty(params.returns()) || (params.returns().size() == 1 && params.returns().contains("e"))) {
      if(!params.isFiltered() && !params.isOrdered() && params.returns() == null && params.page() == 1) {
        if(!CollectionUtils.isEmpty(params.ids())) {
          return params.ids().stream()
            .map(id -> repository.find(id, params.fields()))
            .filter(Objects::nonNull);
        }
        else if(!CollectionUtils.isEmpty(params.uuids())) {
          return params.uuids().stream()
            .map(uuid -> repository.findIndexedSingle(Model.Uuid, uuid, params))
            .filter(Objects::nonNull);
        }
      }

      Result result = repository.queryBuilder().execute(params);
      return StreamUtils.get(new ExecutionResultIterator<>(repository, result, params));
    } else if(params.returns().size() == 1) {
      Result result = repository.queryBuilder().execute(params);
      FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(repository.getModelClass(), 
        params.returns().get(0));
      if(descriptor.isRelationship()) {
        return StreamUtils.get(new RelationshipResultIterator<>(result, params));
      } else {
        try {
          Repository repository = service.repository(descriptor.baseClass().getSimpleName());
          return StreamUtils.get(new ExecutionResultIterator<>(repository, result, params));
        }
        catch(RepositoryInstantiationException e) {
          return StreamUtils.get(new ExecutionResultIterator<>(service, descriptor.baseClass(), result, params));
        }
      }
    } else {
      // TODO: Handle correctly
      throw new UnsupportedOperationException();
    }
  }
  
  public long count() {
    return repository.queryBuilder().count(params);
  }

  public Number sum(String field) {
    return repository.queryBuilder().sum(field, params);
  }
  
  public <T extends Base> Stream<T> stream() {
    return (Stream<T>) search(params);
  }

  public <T extends Base> List<T> list() {
    return ((Stream<T>) search(params)).collect(Collectors.toList());
  }

  public <T extends Base> Set<T> set() {
    return ((Stream<T>) search(params)).collect(Collectors.toSet());
  }

  public <T extends Base> T single() {
    Optional<T> optional = (Optional<T>) search(params.limit(1)).findFirst();
    return optional.isPresent()? optional.get(): null;
  }
  
  public Search params(SearchParameter params) {
    this.params = params;
    return this;
  }

  public Search fields(String... fields) {
    params.fields(fields);
    return this;
  }

  public Search fields(QueryField... fields) {
    params.fields(fields);
    return this;
  }

  public Search fields(FieldList fields) {
    params.fields(fields);
    return this;
  }

  public Search depth(int depth) {
    params.depth(depth);
    return this;
  }

  public Search locale(Locale locale) {
    params.locale(locale);
    return this;
  }

  public Search query(String query) {
    params.query(query);
    return this;
  }

  public Search filter(String property, String value) {
    params.filter(property, value);
    return this;
  }

  public Search filter(String property, Filter filter) {
    params.filter(property, filter);
    return this;
  }

  public Search start(int start) {
    params.start(start);
    return this;
  }

  public Search ids(Long... ids) {
    params.ids(ids);
    return this;
  }

  public Search ids(Set<Long> ids) {
    params.ids(ids);
    return this;
  }

  public Search uuids(String... uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search uuids(List<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search uuids(Set<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  public Search limit(int limit) {
    params.limit(limit);
    return this;
  }

  public Search page(int page) {
    params.page(page);
    return this;
  }

  public Search orderBy(String field) {
    params.orderBy(field);
    return this;
  }

  public Search orderBy(String field, SearchParameter.SortOrder dir) {
    params.orderBy(field, dir);
    return this;
  }

  public Search returns(String... fields) {
    params.returns(fields);
    return this;
  }
}
