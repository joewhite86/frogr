package de.whitefrog.neobase.index;

import de.whitefrog.neobase.exception.QueryParseException;
import org.apache.commons.lang.Validate;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class IndexUtils {
  private static QueryContext buildQuery(String field, String value, boolean allowWildcard, int limit) {
    String query = QueryParser.escape(value).replace(" ", "\\ ");
    if(allowWildcard && value.contains("*")) query = query.replace("\\*", "*");
    if(query.contains("AND") || query.contains("OR") || query.contains("NOT")) query = query.toLowerCase();
    QueryContext context = new QueryContext(field + ":" + query);
    if(limit != Integer.MAX_VALUE) context.top(limit);
    return context;
  }
  
  private static QueryContext buildQuery(String field, String value, boolean allowWildcard, Map<String, String>
    sorting, int limit) {
    QueryContext context = buildQuery(field, value, allowWildcard, limit);

    for(String key: sorting.keySet()) {
      context.sort(key, sorting.get(key));
    }

    return context;
  }

  public static <T extends PropertyContainer> IndexHits<T> query(Index<T> index, String field, String query, int limit) {
    return query(index, field, query.toLowerCase(), new HashMap<>(), limit);
  }

  public static <T extends PropertyContainer> IndexHits<T> query(Index<T> index, String field, String query,
                                                                 Map<String, String> sorting, int limit) {
    Validate.notNull( query );

    try {
      return index.query(buildQuery(field, query, true, sorting, limit));
    } catch(NullPointerException e) {
      throw new QueryParseException(query, e);
    }
  }

  public static <T extends PropertyContainer> T querySingle(Index<T> index, String field, Object value) {
    Validate.notNull(value);
    try(IndexHits<T> hits = index.get(field, value)) {
      if (!hits.hasNext()) return null;
      return hits.getSingle();
    }
    catch(NoSuchElementException e) {
      throw new IllegalStateException(String.format("found more than one element with %s \"%s\"", field, value));
    }
  }
}
