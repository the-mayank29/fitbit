package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * Activity entity representing workouts and physical movement.
 * Extends HealthMetric (Inheritance & Polymorphism).
 * Supports multi-user profiling.
 */
public class Activity extends HealthMetric {
    private String name;
    private String type; // RUNNING, WALKING, CYCLING, SWIMMING, STRENGTH, YOGA, HIIT, PILATES, HIKING, OTHER
    private double durationMinutes;
    private double distanceKm;
    private int caloriesBurned;
    private int steps;
    private int avgHeartRate;
    private int maxHeartRate;
    private String intensity; // LOW, MODERATE, HIGH, EXTREME

    public Activity() {
        super();
        this.name = "Workout";
        this.type = "OTHER";
        this.durationMinutes = 30.0;
        this.caloriesBurned = 150;
        this.intensity = "MODERATE";
    }

    public Activity(String name, String type, String timestamp, double durationMinutes, double distanceKm,
                    int caloriesBurned, int steps, int avgHeartRate, String intensity, String notes) {
        super(timestamp, notes);
        this.name = name;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.caloriesBurned = caloriesBurned;
        this.steps = steps;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = avgHeartRate > 0 ? (int)(avgHeartRate * 1.25) : 0;
        this.intensity = intensity;
    }

    public Activity(String userId, String name, String type, String timestamp, double durationMinutes, double distanceKm,
                    int caloriesBurned, int steps, int avgHeartRate, String intensity, String notes) {
        super(userId, timestamp, notes);
        this.name = name;
        this.type = type;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.caloriesBurned = caloriesBurned;
        this.steps = steps;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = avgHeartRate > 0 ? (int)(avgHeartRate * 1.25) : 0;
        this.intensity = intensity;
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Activity name cannot be empty", "name");
        }
        if (durationMinutes <= 0) {
            throw new ValidationException("Activity duration must be greater than 0 minutes", "durationMinutes");
        }
        if (caloriesBurned < 0) {
            throw new ValidationException("Calories burned cannot be negative", "caloriesBurned");
        }
    }

    @Override
    public String getCategory() {
        return "ACTIVITY_" + (type != null ? type : "OTHER");
    }

    @Override
    public double getPrimaryMetricValue() {
        return caloriesBurned;
    }

    @Override
    public String getSummary() {
        return String.format("%s (%s): %.0f mins, %d kcal burned", name, type, durationMinutes, caloriesBurned);
    }

    @Override
    public String getEntityType() {
        return "ACTIVITY";
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; markUpdated(); }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; markUpdated(); }

    public double getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(double durationMinutes) { this.durationMinutes = durationMinutes; markUpdated(); }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; markUpdated(); }

    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; markUpdated(); }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; markUpdated(); }

    public int getAvgHeartRate() { return avgHeartRate; }
    public void setAvgHeartRate(int avgHeartRate) { this.avgHeartRate = avgHeartRate; markUpdated(); }

    public int getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(int maxHeartRate) { this.maxHeartRate = maxHeartRate; markUpdated(); }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; markUpdated(); }
}
