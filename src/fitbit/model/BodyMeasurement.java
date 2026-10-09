package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * BodyMeasurement entity holding weight, height, body fat, and circumferences.
 * Extends HealthMetric (Inheritance & Polymorphism).
 * Supports multi-user isolation.
 */
public class BodyMeasurement extends HealthMetric {
    private double weightKg;
    private double heightCm;
    private double bmi;
    private String bmiCategory; // UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE
    private double bodyFatPercent;
    private double muscleMassKg;
    private double chestCm;
    private double waistCm;
    private double hipsCm;
    private double bicepsCm;
    private double thighsCm;
    private double waistToHipRatio;

    public BodyMeasurement() {
        super();
        this.weightKg = 65.0;
        this.heightCm = 170.0;
        recalculateMetrics();
    }

    public BodyMeasurement(String timestamp, double weightKg, double heightCm, double bodyFatPercent,
                           double muscleMassKg, double chestCm, double waistCm, double hipsCm,
                           double bicepsCm, double thighsCm, String notes) {
        super(timestamp, notes);
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.bodyFatPercent = bodyFatPercent;
        this.muscleMassKg = muscleMassKg;
        this.chestCm = chestCm;
        this.waistCm = waistCm;
        this.hipsCm = hipsCm;
        this.bicepsCm = bicepsCm;
        this.thighsCm = thighsCm;
        recalculateMetrics();
    }

    public BodyMeasurement(String userId, String timestamp, double weightKg, double heightCm, double bodyFatPercent,
                           double muscleMassKg, double chestCm, double waistCm, double hipsCm,
                           double bicepsCm, double thighsCm, String notes) {
        super(userId, timestamp, notes);
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.bodyFatPercent = bodyFatPercent;
        this.muscleMassKg = muscleMassKg;
        this.chestCm = chestCm;
        this.waistCm = waistCm;
        this.hipsCm = hipsCm;
        this.bicepsCm = bicepsCm;
        this.thighsCm = thighsCm;
        recalculateMetrics();
    }

    public void recalculateMetrics() {
        if (heightCm > 0 && weightKg > 0) {
            double heightM = heightCm / 100.0;
            this.bmi = Math.round((weightKg / (heightM * heightM)) * 10.0) / 10.0;
            if (this.bmi < 18.5) {
                this.bmiCategory = "UNDERWEIGHT";
            } else if (this.bmi < 25.0) {
                this.bmiCategory = "NORMAL";
            } else if (this.bmi < 30.0) {
                this.bmiCategory = "OVERWEIGHT";
            } else {
                this.bmiCategory = "OBESE";
            }
        }
        if (hipsCm > 0 && waistCm > 0) {
            this.waistToHipRatio = Math.round((waistCm / hipsCm) * 100.0) / 100.0;
        }
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (weightKg <= 20 || weightKg > 400) {
            throw new ValidationException("Weight must be between 20kg and 400kg", "weightKg");
        }
        if (heightCm <= 50 || heightCm > 280) {
            throw new ValidationException("Height must be between 50cm and 280cm", "heightCm");
        }
    }

    @Override
    public String getCategory() {
        return "BODY_ANTHROPOMETRY";
    }

    @Override
    public double getPrimaryMetricValue() {
        return weightKg;
    }

    @Override
    public String getSummary() {
        return String.format("%.1f kg (BMI: %.1f, %s)", weightKg, bmi, bmiCategory);
    }

    @Override
    public String getEntityType() {
        return "BODY_MEASUREMENT";
    }

    // Getters and setters
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; recalculateMetrics(); markUpdated(); }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; recalculateMetrics(); markUpdated(); }

    public double getBmi() { return bmi; }
    public void setBmi(double bmi) { this.bmi = bmi; }

    public String getBmiCategory() { return bmiCategory; }
    public void setBmiCategory(String bmiCategory) { this.bmiCategory = bmiCategory; }

    public double getBodyFatPercent() { return bodyFatPercent; }
    public void setBodyFatPercent(double bodyFatPercent) { this.bodyFatPercent = bodyFatPercent; markUpdated(); }

    public double getMuscleMassKg() { return muscleMassKg; }
    public void setMuscleMassKg(double muscleMassKg) { this.muscleMassKg = muscleMassKg; markUpdated(); }

    public double getChestCm() { return chestCm; }
    public void setChestCm(double chestCm) { this.chestCm = chestCm; markUpdated(); }

    public double getWaistCm() { return waistCm; }
    public void setWaistCm(double waistCm) { this.waistCm = waistCm; recalculateMetrics(); markUpdated(); }

    public double getHipsCm() { return hipsCm; }
    public void setHipsCm(double hipsCm) { this.hipsCm = hipsCm; recalculateMetrics(); markUpdated(); }

    public double getBicepsCm() { return bicepsCm; }
    public void setBicepsCm(double bicepsCm) { this.bicepsCm = bicepsCm; markUpdated(); }

    public double getThighsCm() { return thighsCm; }
    public void setThighsCm(double thighsCm) { this.thighsCm = thighsCm; markUpdated(); }

    public double getWaistToHipRatio() { return waistToHipRatio; }
    public void setWaistToHipRatio(double waistToHipRatio) { this.waistToHipRatio = waistToHipRatio; }
}
