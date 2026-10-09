package fitbit.repository;

import fitbit.model.BaseEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic Repository interface defining CRUD operations and collection queries.
 * Demonstrates Java Generics & Collections architecture (6 marks).
 *
 * @param <T>  Entity type bounded by BaseEntity
 * @param <ID> Key type (e.g., String, Long)
 */
public interface Repository<T extends BaseEntity, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean deleteById(ID id);
    boolean existsById(ID id);
    long count();

    /**
     * Generic query method using Java Predicates & Collections.
     */
    List<T> filter(Predicate<T> predicate);

    /**
     * Generic sorting method using Java Comparators.
     */
    List<T> findSorted(Comparator<T> comparator);

    void clear();
}
