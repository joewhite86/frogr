package de.whitefrog.neobase.cypher;

import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.Result;

import java.util.Map;

public interface QueryBuilder {
    StringBuilder start(SearchParameter params, Map<String, Object> queryParams);

    StringBuilder match(SearchParameter params, Map<String, Object> queryParams);

    StringBuilder where(SearchParameter params, Map<String, Object> queryParams);

    StringBuilder orderBy(SearchParameter params);

    Query build(SearchParameter params);

    Number sum(String field, SearchParameter params);

    long count(SearchParameter params);

    Result execute(SearchParameter params);

    Repository repository();

    class Query {
        private String query;
        private Map<String, Object> params;

        public Query(String query, Map<String, Object> params) {
            this.query = query;
            this.params = params;
        }

        public String query() {
            return query;
        }

        public void query(String query) {
            this.query = query;
        }

        public Map<String, Object> params() {
            return params;
        }

        public void params(Map<String, Object> params) {
            this.params = params;
        }
    }
}
