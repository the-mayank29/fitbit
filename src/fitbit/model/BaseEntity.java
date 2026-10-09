package fitbit.model;

import fitbit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract root entity for all domain objects.
 * Demonstrates OOP Inheritance and template contracts.
 */
public abstract class BaseEntity {
    protected String id;
    protected String createdAt;
    protected String updatedAt;

    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now().toString();
        this.updatedAt = this.createdAt;
    }

    public BaseEntity(String id) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now().toString();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now().toString();
    }

    /**
     * Polymorphic contract to validate entity state.
     */
    public abstract void validate() throws ValidationException;

    /**
     * Polymorphic entity type identifier.
     */
    public abstract String getEntityType();
}
