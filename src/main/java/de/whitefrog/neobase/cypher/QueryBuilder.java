package de.whitefrog.neobase.cypher;

import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.repository.Repository;

import java.util.Map;

public interface QueryBuilder {
    Query build(SearchParameter params);

    Query buildSimple(SearchParameter params);

    StringBuilder match(SearchParameter params, Map<String, Object> queryParams);

    StringBuilder orderBy(SearchParameter params);

    Repository repository();

    StringBuilder start(SearchParameter params, Map<String, Object> queryParams);

    StringBuilder where(SearchParameter params, Map<String, Object> queryParams);
}
