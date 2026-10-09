package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * SleepSession entity for sleep architecture, hypnogram stages, and recovery scoring.
 * Extends HealthMetric (Inheritance & Polymorphism).
 */
public class SleepSession extends HealthMetric {
    private String sleepStart;
    private String sleepEnd;
    private int totalMinutes;
    private int deepMinutes;
    private int lightMinutes;
    private int remMinutes;
    private int awakeMinutes;
    private double efficiencyPercent;
    private int sleepScore;
    private String quality;

    public SleepSession() {
        super();
        this.sleepStart = "23:00";
        this.sleepEnd = "07:00";
        this.deepMinutes = 100;
        this.lightMinutes = 240;
        this.remMinutes = 100;
        this.awakeMinutes = 20;
        this.totalMinutes = deepMinutes + lightMinutes + remMinutes;
        calculateSleepScore();
    }

    public SleepSession(String sleepStart, String sleepEnd, int deepMinutes, int lightMinutes,
                        int remMinutes, int awakeMinutes, String notes) {
        super(sleepStart, notes);
        this.sleepStart = sleepStart;
        this.sleepEnd = sleepEnd;
        this.deepMinutes = deepMinutes;
        this.lightMinutes = lightMinutes;
        this.remMinutes = remMinutes;
        this.awakeMinutes = awakeMinutes;
        this.totalMinutes = deepMinutes + lightMinutes + remMinutes;
        calculateSleepScore();
    }

    public void calculateSleepScore() {
        int asleep = deepMinutes + lightMinutes + remMinutes;
        int inBed = asleep + awakeMinutes;
        if (inBed > 0) {
            this.efficiencyPercent = Math.round(((double) asleep / inBed) * 1000.0) / 10.0;
        } else {
            this.efficiencyPercent = 0.0;
        }

        double durationScore = Math.min(50.0, (asleep / 450.0) * 50.0);
        double deepRatio = asleep > 0 ? (double) deepMinutes / asleep : 0;
        double deepScore = Math.min(25.0, (deepRatio / 0.20) * 25.0);

        double remRatio = asleep > 0 ? (double) remMinutes / asleep : 0;
        double remScore = Math.min(20.0, (remRatio / 0.22) * 20.0);

        double penalty = (awakeMinutes > 40) ? Math.min(15.0, (awakeMinutes - 40) * 0.5) : 0;

        int score = (int) Math.round(durationScore + deepScore + remScore - penalty);
        this.sleepScore = Math.max(10, Math.min(100, score));

        if (this.sleepScore >= 85) {
            this.quality = "EXCELLENT";
        } else if (this.sleepScore >= 75) {
            this.quality = "GOOD";
        } else if (this.sleepScore >= 60) {
            this.quality = "FAIR";
        } else {
            this.quality = "POOR";
        }
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (totalMinutes <= 0) {
            throw new ValidationException("Sleep duration must be greater than zero", "totalMinutes");
        }
    }

    @Override
    public String getCategory() {
        return "SLEEP_RECOVERY";
    }

    @Override
    public double getPrimaryMetricValue() {
        return sleepScore;
    }

    @Override
    public String getSummary() {
        return String.format("%dh %dm sleep (Score: %d/100, %s)", totalMinutes / 60, totalMinutes % 60, sleepScore, quality);
    }

    @Override
    public String getEntityType() {
        return "SLEEP_SESSION";
    }

    // Getters and Setters
    public String getSleepStart() { return sleepStart; }
    public void setSleepStart(String sleepStart) { this.sleepStart = sleepStart; setTimestamp(sleepStart); markUpdated(); }

    public String getSleepEnd() { return sleepEnd; }
    public void setSleepEnd(String sleepEnd) { this.sleepEnd = sleepEnd; markUpdated(); }

    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; markUpdated(); }

    public int getDeepMinutes() { return deepMinutes; }
    public void setDeepMinutes(int deepMinutes) { this.deepMinutes = deepMinutes; calculateSleepScore(); markUpdated(); }

    public int getLightMinutes() { return lightMinutes; }
    public void setLightMinutes(int lightMinutes) { this.lightMinutes = lightMinutes; calculateSleepScore(); markUpdated(); }

    public int getRemMinutes() { return remMinutes; }
    public void setRemMinutes(int remMinutes) { this.remMinutes = remMinutes; calculateSleepScore(); markUpdated(); }

    public int getAwakeMinutes() { return awakeMinutes; }
    public void setAwakeMinutes(int awakeMinutes) { this.awakeMinutes = awakeMinutes; calculateSleepScore(); markUpdated(); }

    public double getEfficiencyPercent() { return efficiencyPercent; }
    public void setEfficiencyPercent(double efficiencyPercent) { this.efficiencyPercent = efficiencyPercent; }

    public int getSleepScore() { return sleepScore; }
    public void setSleepScore(int sleepScore) { this.sleepScore = sleepScore; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}
