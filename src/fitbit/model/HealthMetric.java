package fitbit.model;

import fitbit.exception.ValidationException;

import java.time.LocalDateTime;

/**
 * Abstract base class for all recorded health, fitness, and biometric logs.
 * Demonstrates Multi-Level Inheritance (BaseEntity -> HealthMetric -> Concrete Log).
 */
public abstract class HealthMetric extends BaseEntity implements Trackable {
    protected String timestamp;
    protected String notes;

    public HealthMetric() {
        super();
        this.timestamp = LocalDateTime.now().toString();
        this.notes = "";
    }

    public HealthMetric(String timestamp, String notes) {
        super();
        this.timestamp = (timestamp != null && !timestamp.isBlank()) ? timestamp : LocalDateTime.now().toString();
        this.notes = notes != null ? notes : "";
    }

    @Override
    public String getTimestamp() { return timestamp; }

    @Override
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public void validate() throws ValidationException {
        if (timestamp == null || timestamp.isBlank()) {
            throw new ValidationException("Timestamp cannot be empty", "timestamp");
        }
    }
}
