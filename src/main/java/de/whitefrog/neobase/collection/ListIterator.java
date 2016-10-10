package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.repository.Repository;

import java.util.Iterator;

public class ListIterator<T extends de.whitefrog.neobase.model.Model> extends ResultIterator<T> {
    public ListIterator(Repository repository, Iterator<T> iterator) {
        super(repository, iterator);
    }

    @Override
    public boolean hasNext() {
        return baseIterator().hasNext();
    }

    @Override
    public T next() {
        return (T) baseIterator().next();
    }
}
