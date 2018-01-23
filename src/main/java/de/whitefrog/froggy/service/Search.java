package de.whitefrog.froggy.service;

import de.whitefrog.froggy.Service;
import de.whitefrog.froggy.cypher.Query;
import de.whitefrog.froggy.helper.TimeUtils;
import de.whitefrog.froggy.model.Base;
import de.whitefrog.froggy.model.rest.FieldList;
import de.whitefrog.froggy.model.rest.Filter;
import de.whitefrog.froggy.model.rest.QueryField;
import de.whitefrog.froggy.model.rest.SearchParameter;
import de.whitefrog.froggy.persistence.FieldDescriptor;
import de.whitefrog.froggy.persistence.Persistence;
import de.whitefrog.froggy.repository.Repository;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides search filtering, counting, fetching, ordering and paging.
 * Can return results in different formats.
 */
public class Search {
  private static final Logger logger = LoggerFactory.getLogger(Search.class);
  private final Service service;
  private final Repository<? extends Base> repository;
  private SearchParameter params;
  private boolean debugQuery = false;
  
  public Search(Repository<? extends Base> repository) {
    this.repository = repository;
    this.service = repository.service();
    this.params = new SearchParameter();
  }

  public long count() {
    String id = CollectionUtils.isEmpty(params.returns())? "*": params.returns().get(0);
    return count(id);
  }
  public long count(String id) {
    Query query = repository.queryBuilder().buildSimple(params);
    query.query(query.query() + " return count(" + id + ") as c");

    Result result = execute(query);
    long count = (long) result.columnAs("c").next();
    result.close();
    return count;
  }
  
  public Number sum(String field) {
    Query query = repository.queryBuilder().buildSimple(params);
    query.query(query.query() + " return sum(" + field + ") as c");
    Result result = execute(query);
    long sum = (long) result.columnAs("c").next();
    result.close();
    return sum;
  }

  public <T extends Base> List<T> list() {
    Stream<T> stream = (Stream<T>) search(params);
    List<T> list = stream.collect(Collectors.toList());
    stream.close();
    return list;
  }

  public <T extends Base> Set<T> set() {
    Stream<T> stream = (Stream<T>) search(params);
    Set<T> set = stream.collect(Collectors.toSet());
    stream.close();
    return set;
  }

  public <T extends Base> T single() {
    Stream<T> result = (Stream<T>) search(params.limit(1));
    Optional<T> optional = result.findFirst();
    result.close();
    return optional.orElse(null);
  }

  public Long toLong() {
    Query query = repository.queryBuilder().buildSimple(params);
    if(params.returns().size() > 1) {
      throw new UnsupportedOperationException("more than one return parameter is not supported");
    }
    query.query(query.query() + " return " + params.returns().get(0) + " as c");
    Result result = execute(query);
    Object o = result.hasNext()? result.columnAs("c").next(): null;
    result.close();
    if(o == null) return null;
    else if(o instanceof Long) return (Long) o;
    else if(o instanceof Integer) return ((Integer)o).longValue();
    else throw new UnsupportedOperationException(o.getClass().getSimpleName() + " cannot be cast to Long");
  }

  public Integer toInt() {
    Query query = repository.queryBuilder().buildSimple(params);
    if(params.returns().size() > 1) {
      throw new UnsupportedOperationException("more than one return parameter is not supported");
    }
    query.query(query.query() + " return " + params.returns().get(0) + " as c");
    Result result = execute(query);
    Object o = result.hasNext()? result.columnAs("c").next(): null;
    result.close();
    if(o == null) return null;
    else if(o instanceof Long) return ((Long) o).intValue();
    else if(o instanceof Integer) return (Integer) o;
    else throw new UnsupportedOperationException(o.getClass().getSimpleName() + " cannot be cast to Integer");
  }

  private Stream<? extends Base> search(SearchParameter params) {
    Stream<? extends Base> stream;
    Query query = repository.queryBuilder().build(params);
    Result result = execute(query);

    if(CollectionUtils.isEmpty(params.returns()) ||
      (params.returns().size() == 1 && params.returns().contains(repository.queryIdentifier()))) {
      stream = result.stream().map(new ResultMapper<>(repository, params));
    } else if(params.returns().size() == 1) {
      FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(repository.getModelClass(),
        params.returns().get(0));
      Repository<? extends Base> otherRepository = service.repository(descriptor.baseClass().getSimpleName());
      stream = result.stream().map(new ResultMapper<>(otherRepository, params));
    } else {
      // TODO: Handle correctly
      throw new UnsupportedOperationException();
    }

    return stream.onClose(result::close);
  }
  
  private Result execute(Query query) {
    long start = 0;
    if(logger.isDebugEnabled() || debugQuery) {
      start = System.nanoTime();
    }
    try {
      return repository.service().graph().execute(query.query(), query.params());
    } catch(IllegalStateException e) {
      logger.error("On query: " + query.query(), e);
      throw e;
    } finally {
      if(logger.isDebugEnabled()) {
        logger.debug("\n{}\nQuery: {}\nQueryParams: {}\nTime: {}", params, query.query(), query.params(),
          TimeUtils.formatInterval(System.nanoTime() - start, TimeUnit.NANOSECONDS));
      } else if(debugQuery) {
        System.out.println(MessageFormat.format("{0}\nQuery: {1}\nQueryParams: {2}\nTime: {3}", 
          params, query.query(), query.params(),
          TimeUtils.formatInterval(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
      }
    }
  }
  
  

  public Search debug() {
    debugQuery = true;
    return this;
  }

  public Search depth(int depth) {
    params.depth(depth);
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

  public Search filter(String property, String value) {
    params.filter(property, value);
    return this;
  }

  public Search filter(Filter filter) {
    params.filter(filter);
    return this;
  }

  public Search params(SearchParameter params) {
    this.params = params;
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

  static class ResultMapper<T extends Base> implements Function<Map<String, Object>, T> {
    private final SearchParameter params;
    private final Repository<T> repository;
    
    ResultMapper(Repository<T> repository, SearchParameter params) {
      this.repository = repository;
      this.params = params;
    }
    @Override
    public T apply(Map<String, Object> result) {
      String identifier = CollectionUtils.isEmpty(params.returns())?
        repository.queryIdentifier(): params.returns().get(0);
      if(identifier.contains(".")) identifier = identifier.replace(".", "_");
      PropertyContainer node = (PropertyContainer) result.get(identifier);
      T model = repository.createModel(node, params.fieldList());
  
      // when some fields are fetched in query result already they will be added to the model
      if(result.size() > 1) {
        // TODO
      }
      return model;
    }
  }
}
