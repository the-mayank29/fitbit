package fitbit.model;

import fitbit.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * CycleLog entity for menstrual health, symptoms, mood, and cycle predictions.
 * Extends HealthMetric (Inheritance & Polymorphism).
 * Supports multi-user isolation.
 */
public class CycleLog extends HealthMetric {
    private String logDate;
    private String cycleStartDate;
    private int cycleDay;
    private String phase;
    private String flow;
    private List<String> symptoms;
    private String mood;
    private String cervicalMucus;
    private double basalBodyTempC;
    private boolean isFertileWindow;
    private boolean isOvulationDay;
    private String predictedNextPeriod;
    private String predictedOvulationDate;

    public CycleLog() {
        super();
        this.logDate = "2026-10-09";
        this.cycleStartDate = "2026-09-28";
        this.cycleDay = 12;
        this.phase = "FOLLICULAR";
        this.flow = "NONE";
        this.symptoms = new ArrayList<>();
        this.mood = "Happy";
        this.basalBodyTempC = 36.6;
    }

    public CycleLog(String logDate, String cycleStartDate, int cycleDay, String phase, String flow,
                    List<String> symptoms, String mood, String cervicalMucus, double basalBodyTempC,
                    boolean isFertileWindow, boolean isOvulationDay, String predictedNextPeriod,
                    String predictedOvulationDate, String notes) {
        super(logDate, notes);
        this.logDate = logDate;
        this.cycleStartDate = cycleStartDate;
        this.cycleDay = cycleDay;
        this.phase = phase;
        this.flow = flow;
        this.symptoms = symptoms != null ? symptoms : new ArrayList<>();
        this.mood = mood;
        this.cervicalMucus = cervicalMucus;
        this.basalBodyTempC = basalBodyTempC;
        this.isFertileWindow = isFertileWindow;
        this.isOvulationDay = isOvulationDay;
        this.predictedNextPeriod = predictedNextPeriod;
        this.predictedOvulationDate = predictedOvulationDate;
    }

    public CycleLog(String userId, String logDate, String cycleStartDate, int cycleDay, String phase, String flow,
                    List<String> symptoms, String mood, String cervicalMucus, double basalBodyTempC,
                    boolean isFertileWindow, boolean isOvulationDay, String predictedNextPeriod,
                    String predictedOvulationDate, String notes) {
        super(userId, logDate, notes);
        this.logDate = logDate;
        this.cycleStartDate = cycleStartDate;
        this.cycleDay = cycleDay;
        this.phase = phase;
        this.flow = flow;
        this.symptoms = symptoms != null ? symptoms : new ArrayList<>();
        this.mood = mood;
        this.cervicalMucus = cervicalMucus;
        this.basalBodyTempC = basalBodyTempC;
        this.isFertileWindow = isFertileWindow;
        this.isOvulationDay = isOvulationDay;
        this.predictedNextPeriod = predictedNextPeriod;
        this.predictedOvulationDate = predictedOvulationDate;
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (cycleDay <= 0 || cycleDay > 60) {
            throw new ValidationException("Cycle day must be between 1 and 60", "cycleDay");
        }
    }

    @Override
    public String getCategory() {
        return "MENSTRUAL_CYCLE_" + (phase != null ? phase : "TRACK");
    }

    @Override
    public double getPrimaryMetricValue() {
        return cycleDay;
    }

    @Override
    public String getSummary() {
        return String.format("Cycle Day %d (%s phase), Flow: %s, Mood: %s", cycleDay, phase, flow, mood);
    }

    @Override
    public String getEntityType() {
        return "CYCLE_LOG";
    }

    // Getters and Setters
    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; setTimestamp(logDate); markUpdated(); }

    public String getCycleStartDate() { return cycleStartDate; }
    public void setCycleStartDate(String cycleStartDate) { this.cycleStartDate = cycleStartDate; markUpdated(); }

    public int getCycleDay() { return cycleDay; }
    public void setCycleDay(int cycleDay) { this.cycleDay = cycleDay; markUpdated(); }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; markUpdated(); }

    public String getFlow() { return flow; }
    public void setFlow(String flow) { this.flow = flow; markUpdated(); }

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; markUpdated(); }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; markUpdated(); }

    public String getCervicalMucus() { return cervicalMucus; }
    public void setCervicalMucus(String cervicalMucus) { this.cervicalMucus = cervicalMucus; markUpdated(); }

    public double getBasalBodyTempC() { return basalBodyTempC; }
    public void setBasalBodyTempC(double basalBodyTempC) { this.basalBodyTempC = basalBodyTempC; markUpdated(); }

    public boolean isFertileWindow() { return isFertileWindow; }
    public void setFertileWindow(boolean fertileWindow) { isFertileWindow = fertileWindow; }

    public boolean isOvulationDay() { return isOvulationDay; }
    public void setOvulationDay(boolean ovulationDay) { isOvulationDay = ovulationDay; }

    public String getPredictedNextPeriod() { return predictedNextPeriod; }
    public void setPredictedNextPeriod(String predictedNextPeriod) { this.predictedNextPeriod = predictedNextPeriod; }

    public String getPredictedOvulationDate() { return predictedOvulationDate; }
    public void setPredictedOvulationDate(String predictedOvulationDate) { this.predictedOvulationDate = predictedOvulationDate; }
}
