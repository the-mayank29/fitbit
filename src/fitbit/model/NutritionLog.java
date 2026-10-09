package fitbit.model;

import fitbit.exception.ValidationException;

/**
 * NutritionLog entity for food, macros, and hydration.
 * Extends HealthMetric (Inheritance & Polymorphism).
 */
public class NutritionLog extends HealthMetric {
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK, WATER
    private String foodName;
    private int calories;
    private double proteinGrams;
    private double carbsGrams;
    private double fatGrams;
    private double fiberGrams;
    private int waterMl;

    public NutritionLog() {
        super();
        this.mealType = "SNACK";
        this.foodName = "Healthy Snack";
        this.calories = 150;
    }

    public NutritionLog(String timestamp, String mealType, String foodName, int calories,
                        double proteinGrams, double carbsGrams, double fatGrams, double fiberGrams,
                        int waterMl, String notes) {
        super(timestamp, notes);
        this.mealType = mealType;
        this.foodName = foodName;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.fiberGrams = fiberGrams;
        this.waterMl = waterMl;
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (foodName == null || foodName.trim().isEmpty()) {
            throw new ValidationException("Food name cannot be empty", "foodName");
        }
        if (calories < 0) {
            throw new ValidationException("Calories cannot be negative", "calories");
        }
    }

    @Override
    public String getCategory() {
        return "NUTRITION_" + (mealType != null ? mealType : "MEAL");
    }

    @Override
    public double getPrimaryMetricValue() {
        return calories;
    }

    @Override
    public String getSummary() {
        return String.format("[%s] %s: %d kcal (P:%.1fg, C:%.1fg, F:%.1fg)", mealType, foodName, calories, proteinGrams, carbsGrams, fatGrams);
    }

    @Override
    public String getEntityType() {
        return "NUTRITION_LOG";
    }

    // Getters and Setters
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; markUpdated(); }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; markUpdated(); }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; markUpdated(); }

    public double getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(double proteinGrams) { this.proteinGrams = proteinGrams; markUpdated(); }

    public double getCarbsGrams() { return carbsGrams; }
    public void setCarbsGrams(double carbsGrams) { this.carbsGrams = carbsGrams; markUpdated(); }

    public double getFatGrams() { return fatGrams; }
    public void setFatGrams(double fatGrams) { this.fatGrams = fatGrams; markUpdated(); }

    public double getFiberGrams() { return fiberGrams; }
    public void setFiberGrams(double fiberGrams) { this.fiberGrams = fiberGrams; markUpdated(); }

    public int getWaterMl() { return waterMl; }
    public void setWaterMl(int waterMl) { this.waterMl = waterMl; markUpdated(); }
}
