package de.whitefrog.frogr.service;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.cypher.Query;
import de.whitefrog.frogr.helper.TimeUtils;
import de.whitefrog.frogr.model.Base;
import de.whitefrog.frogr.model.rest.FieldList;
import de.whitefrog.frogr.model.rest.Filter;
import de.whitefrog.frogr.model.rest.QueryField;
import de.whitefrog.frogr.model.rest.SearchParameter;
import de.whitefrog.frogr.persistence.FieldDescriptor;
import de.whitefrog.frogr.persistence.Persistence;
import de.whitefrog.frogr.repository.Repository;
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
 * Use as follows:
 * <pre>
 *   <code>List<ModelType> results = repository.search()
 *       .filter("field", "value*")
 *       .fields("fieldA", "fieldB")
 *       .limit(10)
 *       .page(2)
 *       .list();</code>
 * </pre>
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

  /**
   * Count the number of results.
   * @return The result count
   */
  public long count() {
    String id = CollectionUtils.isEmpty(params.returns())? "*": params.returns().get(0);
    return count(id);
  }

  /**
   * Count the number of results of a specific identifier.
   * @param id The identifier to use
   * @return The result count
   */
  public long count(String id) {
    Query query = repository.queryBuilder().buildSimple(params);
    query.query(query.query() + " return count(" + id + ") as c");

    Result result = execute(query);
    long count = (long) result.columnAs("c").next();
    result.close();
    return count;
  }

  /**
   * Adds all found field values.
   * @param field The field to sum up
   * @return The sum of all results
   */
  public Number sum(String field) {
    Query query = repository.queryBuilder().buildSimple(params);
    query.query(query.query() + " return sum(" + field + ") as c");
    Result result = execute(query);
    long sum = (long) result.columnAs("c").next();
    result.close();
    return sum;
  }

  /**
   * Get a {@link List list} of results.
   * @return A {@link List list} of results
   */
  public <T extends Base> List<T> list() {
    Stream<T> stream = (Stream<T>) search(params);
    List<T> list = stream.collect(Collectors.toList());
    stream.close();
    return list;
  }

  /**
   * Get a {@link Set set} of results.
   * @return A {@link Set set} of results
   */
  public <T extends Base> Set<T> set() {
    Stream<T> stream = (Stream<T>) search(params);
    Set<T> set = stream.collect(Collectors.toSet());
    stream.close();
    return set;
  }

  /**
   * Get a single result.
   * @return A single result
   */
  public <T extends Base> T single() {
    Stream<T> result = (Stream<T>) search(params.limit(1));
    Optional<T> optional = result.findFirst();
    result.close();
    return optional.orElse(null);
  }

  /**
   * Get a {@link Long} value. Tries to convert the result to {@link Long} and throws an 
   * {@link UnsupportedOperationException} if that fails.
   * @return A {@link Long} value
   */
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

  /**
   * Get a {@link Integer} value. Tries to convert the result to {@link Integer} and 
   * throws an {@link UnsupportedOperationException} if that fails.
   * @return A {@link Integer} value
   */
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


  /**
   * Print queries and parameters to stdout.
   */
  public Search debug() {
    debugQuery = true;
    return this;
  }

  public Search depth(int depth) {
    params.depth(depth);
    return this;
  }

  /**
   * Fields to fetch on results.
   * @param fields Fields to fetch
   */
  public Search fields(String... fields) {
    params.fields(FieldList.parseFields(fields));
    return this;
  }

  /**
   * Fields to fetch on results.
   * @param fields {@link QueryField}'s to fetch
   */
  public Search fields(QueryField... fields) {
    params.fields(fields);
    return this;
  }

  /**
   * Fields to fetch on results.
   * @param fields {@link FieldList} containing all fields to fetch
   */
  public Search fields(FieldList fields) {
    params.fields(fields);
    return this;
  }

  /**
   * Filter results by field values. Supports only {@link Filter.Equals} filter.
   * @param property Field to apply the filter on
   * @param value Value to look for
   */
  public Search filter(String property, String value) {
    params.filter(property, value);
    return this;
  }

  /**
   * Adds a {@link Filter filter} for results.
   * @param filter {@link Filter} to add
   */
  public Search filter(Filter filter) {
    params.filter(filter);
    return this;
  }

  /**
   * Replaces the underlying {@link SearchParameter} object.
   * This should be called first, to prevent overriding.
   * @param params {@link SearchParameter} object
   */
  public Search params(SearchParameter params) {
    this.params = params;
    return this;
  }

  /**
   * {@link Locale} to use for queries.
   * @param locale {@link Locale} to use
   */
  public Search locale(Locale locale) {
    params.locale(locale);
    return this;
  }

  /**
   * Query indexed fields. 
   * Use '*' as wildcard operator at start or end of query.
   * Use the form 'field:queryString' to query only specific fields.
   * Could be replaced by {@link Filter filters}. But this provides a convenient way to make simple queries.
   * @param query The querystring to use
   */
  public Search query(String query) {
    params.query(query);
    return this;
  }

  /**
   * Start results at a specific position. Especially useful in combination
   * when results are {@link #orderBy(String, SearchParameter.SortOrder) ordered}.
   * @param start The position to start from.
   */
  public Search start(int start) {
    params.start(start);
    return this;
  }

  /**
   * Filter results by ids.
   * @param ids Ids to include in results
   */
  public Search ids(Long... ids) {
    params.ids(ids);
    return this;
  }

  /**
   * Filter results by ids.
   * @param ids Ids to include in results as set
   */
  public Search ids(Set<Long> ids) {
    params.ids(ids);
    return this;
  }

  /**
   * Filter results by uuids.
   * @param uuids UUIDs to include in results
   */
  public Search uuids(String... uuids) {
    params.uuids(uuids);
    return this;
  }

  /**
   * Filter results by uuids.
   * @param uuids UUIDs to include in results as list
   */
  public Search uuids(List<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  /**
   * Filter results by uuids.
   * @param uuids UUIDs to include in results as set
   */
  public Search uuids(Set<String> uuids) {
    params.uuids(uuids);
    return this;
  }

  /**
   * Limit results to a specific count.
   * @param limit Amount of results to return
   */
  public Search limit(int limit) {
    params.limit(limit);
    return this;
  }

  /**
   * Page number to return, starts with 1.
   * Should be used in combination with {@link #limit(int) limit}.
   * If set, there's no {@link #start(int) start} required.
   * @param page Page number
   */
  public Search page(int page) {
    params.page(page);
    return this;
  }

  /**
   * Sort results by a specific field in ascending order.
   * @param field Field, by which results will be sorted
   */
  public Search orderBy(String field) {
    params.orderBy(field);
    return this;
  }

  /**
   * Sort results by a specific field and given order.
   * @param field Field, by which results will be sorted
   * @param dir Sort direction (ascending or descending)
   */
  public Search orderBy(String field, SearchParameter.SortOrder dir) {
    params.orderBy(field, dir);
    return this;
  }

  /**
   * Fields to return.
   * @param fields Fields to return
   */
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
