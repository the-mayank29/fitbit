package fitbit.model;

import fitbit.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * Abstract base class for all recorded health, fitness, and biometric logs.
 * Demonstrates Multi-Level Inheritance (BaseEntity -> HealthMetric -> Concrete Log).
 * Encapsulates multi-user association via userId.
 */
public abstract class HealthMetric extends BaseEntity implements Trackable {
    protected String userId;
    protected String timestamp;
    protected String notes;

    public HealthMetric() {
        super();
        this.userId = "user_1";
        this.timestamp = LocalDateTime.now().toString();
        this.notes = "";
    }

    public HealthMetric(String timestamp, String notes) {
        super();
        this.userId = "user_1";
        this.timestamp = (timestamp != null && !timestamp.isBlank()) ? timestamp : LocalDateTime.now().toString();
        this.notes = notes != null ? notes : "";
    }

    public HealthMetric(String userId, String timestamp, String notes) {
        super();
        this.userId = (userId != null && !userId.isBlank()) ? userId : "user_1";
        this.timestamp = (timestamp != null && !timestamp.isBlank()) ? timestamp : LocalDateTime.now().toString();
        this.notes = notes != null ? notes : "";
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; markUpdated(); }

    @Override
    public String getTimestamp() { return timestamp; }

    @Override
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; markUpdated(); }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; markUpdated(); }

    @Override
    public void validate() throws ValidationException {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User ID must be associated with the metric", "userId");
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new ValidationException("Timestamp cannot be empty", "timestamp");
        }
    }
}
