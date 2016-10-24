package de.whitefrog.neobase.cypher;

import java.util.Map;

public class Query {
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
