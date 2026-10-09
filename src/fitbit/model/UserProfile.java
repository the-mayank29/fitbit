package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * User Profile entity holding user settings, personal bio, and daily health goals.
 * Extends BaseEntity.
 */
public class UserProfile extends BaseEntity {
    private String name;
    private int age;
    private String gender; // "FEMALE", "MALE", "OTHER"
    private double heightCm;
    private double targetWeightKg;
    private int dailyStepGoal;
    private int dailyCalorieGoal;
    private int dailyWaterGoalMl;
    private double sleepTargetHours;
    private int cycleLengthDays; // default 28
    private int periodLengthDays; // default 5

    public UserProfile() {
        super("user_default");
        this.name = "Alex Morgan";
        this.age = 28;
        this.gender = "FEMALE";
        this.heightCm = 168.0;
        this.targetWeightKg = 62.0;
        this.dailyStepGoal = 10000;
        this.dailyCalorieGoal = 600;
        this.dailyWaterGoalMl = 2500;
        this.sleepTargetHours = 8.0;
        this.cycleLengthDays = 28;
        this.periodLengthDays = 5;
    }

    @Override
    public void validate() throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("User name cannot be empty", "name");
        }
        if (age <= 0 || age > 120) {
            throw new ValidationException("Age must be between 1 and 120", "age");
        }
        if (heightCm <= 0 || heightCm > 300) {
            throw new ValidationException("Height must be positive and realistic", "heightCm");
        }
        if (dailyStepGoal < 500) {
            throw new ValidationException("Daily step goal must be at least 500", "dailyStepGoal");
        }
    }

    @Override
    public String getEntityType() {
        return "USER_PROFILE";
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; markUpdated(); }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; markUpdated(); }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; markUpdated(); }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; markUpdated(); }

    public double getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(double targetWeightKg) { this.targetWeightKg = targetWeightKg; markUpdated(); }

    public int getDailyStepGoal() { return dailyStepGoal; }
    public void setDailyStepGoal(int dailyStepGoal) { this.dailyStepGoal = dailyStepGoal; markUpdated(); }

    public int getDailyCalorieGoal() { return dailyCalorieGoal; }
    public void setDailyCalorieGoal(int dailyCalorieGoal) { this.dailyCalorieGoal = dailyCalorieGoal; markUpdated(); }

    public int getDailyWaterGoalMl() { return dailyWaterGoalMl; }
    public void setDailyWaterGoalMl(int dailyWaterGoalMl) { this.dailyWaterGoalMl = dailyWaterGoalMl; markUpdated(); }

    public double getSleepTargetHours() { return sleepTargetHours; }
    public void setSleepTargetHours(double sleepTargetHours) { this.sleepTargetHours = sleepTargetHours; markUpdated(); }

    public int getCycleLengthDays() { return cycleLengthDays; }
    public void setCycleLengthDays(int cycleLengthDays) { this.cycleLengthDays = cycleLengthDays; markUpdated(); }

    public int getPeriodLengthDays() { return periodLengthDays; }
    public void setPeriodLengthDays(int periodLengthDays) { this.periodLengthDays = periodLengthDays; markUpdated(); }
}
