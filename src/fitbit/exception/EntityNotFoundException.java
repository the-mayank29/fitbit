package fitbit.exception;

/**
 * Thrown when an entity cannot be found in the repository or database.
 */
public class EntityNotFoundException extends FitnessException {
    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " with ID '" + entityId + "' was not found.", "ENTITY_NOT_FOUND");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
