package fitbit.analytics;

import fitbit.model.BodyMeasurement;
import fitbit.model.UserProfile;
import fitbit.model.Vital;

public class HealthCalculator {

    /**
     * Calculate Basal Metabolic Rate using Mifflin-St Jeor Equation
     */
    public static double calculateBMR(double weightKg, double heightCm, int age, String gender) {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 1500.0;
        double s = "MALE".equalsIgnoreCase(gender) ? 5.0 : -161.0;
        return (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + s;
    }

    /**
     * Calculate Total Daily Energy Expenditure (TDEE) based on activity level
     */
    public static double calculateTDEE(double bmr, String activityLevel) {
        double multiplier = switch (activityLevel != null ? activityLevel.toUpperCase() : "MODERATE") {
            case "SEDENTARY" -> 1.2;
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "VERY_ACTIVE" -> 1.725;
            case "EXTRA_ACTIVE" -> 1.9;
            default -> 1.45;
        };
        return Math.round(bmr * multiplier);
    }

    /**
     * Calculate Recommended Daily Hydration (ml)
     * Standard: ~35ml per kg of bodyweight + 350ml per 30min workout
     */
    public static int calculateHydrationGoalMl(double weightKg, double workoutMinutesToday) {
        if (weightKg <= 0) return 2500;
        int baseMl = (int) (weightKg * 35.0);
        int extraWorkoutMl = (int) ((workoutMinutesToday / 30.0) * 350.0);
        return Math.max(1500, baseMl + extraWorkoutMl);
    }

    /**
     * Calculate Heart Rate Target Training Zones based on age
     */
    public static HrZones calculateHrZones(int age) {
        int maxHr = 220 - (age > 0 ? age : 28);
        return new HrZones(
            maxHr,
            (int) (maxHr * 0.50), // Resting / Warm-up lower
            (int) (maxHr * 0.60), // Fat Burn start
            (int) (maxHr * 0.70), // Cardio / Aerobic start
            (int) (maxHr * 0.85), // Anaerobic / Peak start
            maxHr
        );
    }

    public record HrZones(int maxHr, int warmUp, int fatBurn, int cardio, int peak, int redline) {}

    /**
     * Composite Wellness Index (0 to 100)
     */
    public static int calculateWellnessScore(int activitySteps, int stepGoal,
                                            int sleepScore,
                                            int caloriesConsumed, int caloriesBurned, int calorieGoal,
                                            int restingHr, double spO2) {
        double actScore = Math.min(100.0, (double) activitySteps / Math.max(1, stepGoal) * 100.0);
        double slpScore = sleepScore > 0 ? sleepScore : 75.0;
        
        // Vitals score
        double vitalsScore = 80.0;
        if (restingHr >= 55 && restingHr <= 75) vitalsScore += 10.0;
        if (spO2 >= 95.0) vitalsScore += 10.0;

        // Nutrition balance score
        double calScore = 80.0;
        if (caloriesBurned >= calorieGoal) calScore += 15.0;

        double overall = (actScore * 0.35) + (slpScore * 0.30) + (vitalsScore * 0.20) + (calScore * 0.15);
        return (int) Math.max(10, Math.min(100, Math.round(overall)));
    }
}
