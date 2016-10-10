package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.repository.Repository;
import org.neo4j.graphdb.Result;

public abstract class Iterables {
    public static <T extends Model> ResultIterable<T> get(ResultIterator<T> iterator) {
        return new ResultIterable<>(iterator);
    }

    public static <T extends Model> ResultIterable<T> get(
      Repository<T> repository, Result result, String identifier) {
        return new ResultIterable<>(new ExecutionResultIterator<>(repository, result, identifier));
    }
}
