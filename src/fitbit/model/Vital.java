package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * Vital sign entity for cardiovascular and metabolic markers.
 * Extends HealthMetric (Inheritance & Polymorphism).
 */
public class Vital extends HealthMetric {
    private int heartRateBpm;
    private int restingHeartRateBpm;
    private int systolicBp;
    private int diastolicBp;
    private double spO2Percent;
    private double bloodGlucoseMgDl;
    private double bodyTempC;
    private String bpCategory;
    private String hrZone;

    public Vital() {
        super();
        this.heartRateBpm = 72;
        this.restingHeartRateBpm = 62;
        this.systolicBp = 118;
        this.diastolicBp = 76;
        this.spO2Percent = 99.0;
        this.bloodGlucoseMgDl = 92.0;
        this.bodyTempC = 36.6;
        evaluateCategories();
    }

    public Vital(String timestamp, int heartRateBpm, int restingHeartRateBpm, int systolicBp,
                 int diastolicBp, double spO2Percent, double bloodGlucoseMgDl, double bodyTempC, String notes) {
        super(timestamp, notes);
        this.heartRateBpm = heartRateBpm;
        this.restingHeartRateBpm = restingHeartRateBpm;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.spO2Percent = spO2Percent;
        this.bloodGlucoseMgDl = bloodGlucoseMgDl;
        this.bodyTempC = bodyTempC;
        evaluateCategories();
    }

    public void evaluateCategories() {
        if (systolicBp > 0 && diastolicBp > 0) {
            if (systolicBp > 180 || diastolicBp > 120) {
                this.bpCategory = "HYPERTENSIVE_CRISIS";
            } else if (systolicBp >= 140 || diastolicBp >= 90) {
                this.bpCategory = "HYPERTENSION_STAGE_2";
            } else if (systolicBp >= 130 || diastolicBp >= 80) {
                this.bpCategory = "HYPERTENSION_STAGE_1";
            } else if (systolicBp >= 120 && diastolicBp < 80) {
                this.bpCategory = "ELEVATED";
            } else {
                this.bpCategory = "NORMAL";
            }
        } else {
            this.bpCategory = "NOT_RECORDED";
        }

        if (heartRateBpm > 0) {
            if (heartRateBpm < 60) {
                this.hrZone = "BRADYCARDIA_OR_ATHLETE";
            } else if (heartRateBpm <= 100) {
                this.hrZone = "NORMAL_RESTING";
            } else if (heartRateBpm <= 135) {
                this.hrZone = "FAT_BURN";
            } else if (heartRateBpm <= 165) {
                this.hrZone = "CARDIO";
            } else {
                this.hrZone = "PEAK";
            }
        }
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (heartRateBpm <= 20 || heartRateBpm > 250) {
            throw new ValidationException("Heart rate must be between 20 and 250 BPM", "heartRateBpm");
        }
        if (systolicBp < 50 || systolicBp > 300) {
            throw new ValidationException("Systolic BP must be between 50 and 300 mmHg", "systolicBp");
        }
        if (diastolicBp < 30 || diastolicBp > 200) {
            throw new ValidationException("Diastolic BP must be between 30 and 200 mmHg", "diastolicBp");
        }
    }

    @Override
    public String getCategory() {
        return "CARDIOVASCULAR_VITALS";
    }

    @Override
    public double getPrimaryMetricValue() {
        return heartRateBpm;
    }

    @Override
    public String getSummary() {
        return String.format("%d BPM (Resting: %d), BP %d/%d mmHg (%s)", heartRateBpm, restingHeartRateBpm, systolicBp, diastolicBp, bpCategory);
    }

    @Override
    public String getEntityType() {
        return "VITAL";
    }

    // Getters and setters
    public int getHeartRateBpm() { return heartRateBpm; }
    public void setHeartRateBpm(int heartRateBpm) { this.heartRateBpm = heartRateBpm; evaluateCategories(); markUpdated(); }

    public int getRestingHeartRateBpm() { return restingHeartRateBpm; }
    public void setRestingHeartRateBpm(int restingHeartRateBpm) { this.restingHeartRateBpm = restingHeartRateBpm; markUpdated(); }

    public int getSystolicBp() { return systolicBp; }
    public void setSystolicBp(int systolicBp) { this.systolicBp = systolicBp; evaluateCategories(); markUpdated(); }

    public int getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(int diastolicBp) { this.diastolicBp = diastolicBp; evaluateCategories(); markUpdated(); }

    public double getSpO2Percent() { return spO2Percent; }
    public void setSpO2Percent(double spO2Percent) { this.spO2Percent = spO2Percent; markUpdated(); }

    public double getBloodGlucoseMgDl() { return bloodGlucoseMgDl; }
    public void setBloodGlucoseMgDl(double bloodGlucoseMgDl) { this.bloodGlucoseMgDl = bloodGlucoseMgDl; markUpdated(); }

    public double getBodyTempC() { return bodyTempC; }
    public void setBodyTempC(double bodyTempC) { this.bodyTempC = bodyTempC; markUpdated(); }

    public String getBpCategory() { return bpCategory; }
    public void setBpCategory(String bpCategory) { this.bpCategory = bpCategory; }

    public String getHrZone() { return hrZone; }
    public void setHrZone(String hrZone) { this.hrZone = hrZone; }
}
