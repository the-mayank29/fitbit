package fitbit.repository;

import fitbit.model.BaseEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Thread-safe Generic in-memory repository implementation using Java Collections.
 * Utilizes ConcurrentHashMap, ArrayList, and Generic Collections Streams.
 *
 * @param <T>  Entity type extending BaseEntity
 * @param <ID> Identifier type
 */
public class GenericRepository<T extends BaseEntity, ID> implements Repository<T, ID> {
    private final Map<ID, T> storage = new ConcurrentHashMap<>();

    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        @SuppressWarnings("unchecked")
        ID id = (ID) entity.getId();
        storage.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(ID id) {
        if (id == null) return false;
        return storage.remove(id) != null;
    }

    @Override
    public boolean existsById(ID id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public List<T> filter(Predicate<T> predicate) {
        return storage.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public List<T> findSorted(Comparator<T> comparator) {
        return storage.values().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
