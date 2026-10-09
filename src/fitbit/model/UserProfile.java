package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * User Profile entity holding user identity, personal biometric settings, and daily goals.
 * Extends BaseEntity to support multi-user database architecture.
 */
public class UserProfile extends BaseEntity {
    private String name;
    private String email;
    private String avatarColor; // e.g. "#1a73e8", "#00875a", "#d93025", "#9334e6"
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
        super("user_1");
        this.name = "Alex Morgan";
        this.email = "alex.morgan@fitbit.app";
        this.avatarColor = "#1a73e8";
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

    public UserProfile(String id, String name, String email, String avatarColor, int age, String gender,
                       double heightCm, double targetWeightKg, int dailyStepGoal, int dailyCalorieGoal,
                       int dailyWaterGoalMl, double sleepTargetHours, int cycleLengthDays, int periodLengthDays) {
        super(id);
        this.name = name;
        this.email = email != null ? email : (id + "@fitbit.app");
        this.avatarColor = avatarColor != null ? avatarColor : "#1a73e8";
        this.age = age;
        this.gender = gender;
        this.heightCm = heightCm;
        this.targetWeightKg = targetWeightKg;
        this.dailyStepGoal = dailyStepGoal;
        this.dailyCalorieGoal = dailyCalorieGoal;
        this.dailyWaterGoalMl = dailyWaterGoalMl;
        this.sleepTargetHours = sleepTargetHours;
        this.cycleLengthDays = cycleLengthDays;
        this.periodLengthDays = periodLengthDays;
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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; markUpdated(); }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; markUpdated(); }

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
