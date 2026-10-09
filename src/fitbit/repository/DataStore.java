package fitbit.repository;

import fitbit.analytics.CyclePredictor;
import fitbit.analytics.HealthCalculator;
import fitbit.model.*;
import fitbit.server.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore {
    private static final String DATA_FILE = "data/fitness_data.json";

    private UserProfile profile;
    private final List<Activity> activities = new CopyOnWriteArrayList<>();
    private final List<BodyMeasurement> measurements = new CopyOnWriteArrayList<>();
    private final List<Vital> vitals = new CopyOnWriteArrayList<>();
    private final List<NutritionLog> nutritionLogs = new CopyOnWriteArrayList<>();
    private final List<SleepSession> sleepSessions = new CopyOnWriteArrayList<>();
    private final List<CycleLog> cycleLogs = new CopyOnWriteArrayList<>();

    public DataStore() {
        this.profile = new UserProfile();
        loadFromFile();
        if (activities.isEmpty() && measurements.isEmpty()) {
            seedDemoData();
            saveToFile();
        }
    }

    // ==========================================
    // SEED DEMO DATA
    // ==========================================
    public void seedDemoData() {
        activities.clear();
        measurements.clear();
        vitals.clear();
        nutritionLogs.clear();
        sleepSessions.clear();
        cycleLogs.clear();

        this.profile = new UserProfile();
        this.profile.setName("Alex Morgan");
        this.profile.setAge(28);
        this.profile.setGender("FEMALE");
        this.profile.setHeightCm(168.0);
        this.profile.setTargetWeightKg(60.0);
        this.profile.setDailyStepGoal(10000);
        this.profile.setDailyCalorieGoal(600);
        this.profile.setDailyWaterGoalMl(2500);
        this.profile.setCycleLengthDays(28);
        this.profile.setPeriodLengthDays(5);

        LocalDate today = LocalDate.now();

        // 1. Activities (Past 6 days + today)
        activities.add(new Activity("Morning Sunrise Run", "RUNNING", today.minusDays(5).toString() + "T07:15:00", 35.0, 5.2, 380, 5800, 142, "HIGH", "Felt energized, cool morning breeze"));
        activities.add(new Activity("Power Vinyasa Yoga", "YOGA", today.minusDays(4).toString() + "T18:00:00", 45.0, 0.0, 190, 1200, 105, "MODERATE", "Focus on hip openers and core balance"));
        activities.add(new Activity("HIIT Intervals", "HIIT", today.minusDays(3).toString() + "T08:00:00", 30.0, 3.1, 340, 4200, 158, "EXTREME", "Tabata style sprint and kettlebells"));
        activities.add(new Activity("Evening Sunset Walk", "WALKING", today.minusDays(2).toString() + "T19:30:00", 40.0, 3.8, 180, 4900, 98, "LOW", "Recovery walk around the park"));
        activities.add(new Activity("Full Body Strength", "STRENGTH", today.minusDays(1).toString() + "T17:30:00", 50.0, 0.0, 310, 2100, 128, "HIGH", "Squats 60kg, Deadlifts 70kg, Bench press 35kg"));
        activities.add(new Activity("Outdoor Road Cycling", "CYCLING", today.toString() + "T09:00:00", 45.0, 16.5, 420, 6800, 138, "HIGH", "Good cadence, hilly circuit completed"));

        // 2. Body Measurements
        measurements.add(new BodyMeasurement(today.minusDays(14).toString(), 63.8, 168.0, 22.8, 46.2, 88.0, 71.5, 96.0, 27.5, 54.0, "Starting new training cycle"));
        measurements.add(new BodyMeasurement(today.minusDays(7).toString(), 63.1, 168.0, 22.4, 46.5, 87.5, 70.8, 95.5, 27.8, 53.6, "Feeling leaner and stronger"));
        measurements.add(new BodyMeasurement(today.toString(), 62.4, 168.0, 21.9, 46.8, 87.0, 69.8, 95.0, 28.0, 53.2, "Waist down 1.7cm! Good muscle retention."));

        // 3. Vitals
        vitals.add(new Vital(today.minusDays(3).toString() + "T08:00:00", 72, 62, 118, 76, 99.0, 92.0, 36.6, "Morning baseline"));
        vitals.add(new Vital(today.minusDays(2).toString() + "T08:15:00", 68, 60, 116, 75, 98.5, 94.0, 36.5, "Rest day vitals steady"));
        vitals.add(new Vital(today.minusDays(1).toString() + "T08:00:00", 70, 61, 119, 78, 99.0, 90.0, 36.6, "Well recovered after sleep"));
        vitals.add(new Vital(today.toString() + "T08:30:00", 65, 59, 115, 74, 99.5, 89.0, 36.7, "Resting HR at optimal 59 BPM"));

        // 4. Nutrition
        nutritionLogs.add(new NutritionLog(today.toString() + "T08:00:00", "BREAKFAST", "Steel Cut Oats with Blueberries, Chia & Whey", 410, 28.0, 52.0, 8.5, 9.0, 500, "Good complex carbs"));
        nutritionLogs.add(new NutritionLog(today.toString() + "T12:45:00", "LUNCH", "Grilled Lemon Herb Salmon, Quinoa & Steamed Asparagus", 580, 42.0, 38.0, 18.0, 6.0, 600, "High omega-3 & protein"));
        nutritionLogs.add(new NutritionLog(today.toString() + "T16:00:00", "SNACK", "Greek Yogurt (0%) with Almonds & Honey", 220, 18.0, 16.0, 7.0, 2.0, 400, "Afternoon boost"));
        nutritionLogs.add(new NutritionLog(today.toString() + "T19:30:00", "DINNER", "Turkey Stir-Fry with Brown Rice & Broccoli", 520, 38.0, 48.0, 11.0, 7.0, 500, "Clean evening meal"));

        // 5. Sleep Sessions
        sleepSessions.add(new SleepSession(today.minusDays(3).toString() + "T23:00:00", today.minusDays(2).toString() + "T07:00:00", 105, 240, 95, 20, "Woke up feeling refreshed"));
        sleepSessions.add(new SleepSession(today.minusDays(2).toString() + "T23:30:00", today.minusDays(1).toString() + "T06:50:00", 90, 220, 85, 35, "Slightly interrupted early morning"));
        sleepSessions.add(new SleepSession(today.minusDays(1).toString() + "T22:45:00", today.toString() + "T07:15:00", 115, 260, 105, 15, "Deep restorative sleep! High REM recovery"));

        // 6. Cycle Logs (Menstrual cycle: anchor set 10 days ago -> current day 11, follicular phase leading to ovulation)
        LocalDate cycleAnchor = today.minusDays(10);
        cycleLogs.add(new CycleLog(cycleAnchor.toString(), cycleAnchor.toString(), 1, "MENSTRUAL", "MEDIUM",
            List.of("Mild Cramps", "Fatigue"), "Calm", "STICKY", 36.4, false, false,
            cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Period started on schedule"));
        cycleLogs.add(new CycleLog(cycleAnchor.plusDays(2).toString(), cycleAnchor.toString(), 3, "MENSTRUAL", "LIGHT",
            List.of("Mild Bloating"), "Low Energy", "STICKY", 36.4, false, false,
            cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Flow tapering off"));
        cycleLogs.add(new CycleLog(cycleAnchor.plusDays(5).toString(), cycleAnchor.toString(), 6, "FOLLICULAR", "NONE",
            List.of("High Energy"), "Energetic", "CREAMY", 36.5, false, false,
            cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Energy surging, great workout"));
        cycleLogs.add(new CycleLog(today.toString(), cycleAnchor.toString(), 11, "FOLLICULAR", "NONE",
            List.of("Productive", "Clear Skin"), "Happy", "EGG_WHITE", 36.6, true, false,
            cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Approaching fertile window. Mood elevated."));
    }

    // ==========================================
    // PERSISTENCE
    // ==========================================
    public synchronized void saveToFile() {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("profile", profile);
            root.put("activities", activities);
            root.put("measurements", measurements);
            root.put("vitals", vitals);
            root.put("nutritionLogs", nutritionLogs);
            root.put("sleepSessions", sleepSessions);
            root.put("cycleLogs", cycleLogs);

            String json = JsonUtil.toJson(root);
            Path path = Path.of(DATA_FILE);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, json);
        } catch (Exception e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try {
            String content = Files.readString(file.toPath());
            Map<String, Object> root = JsonUtil.parseObject(content);
            if (root == null || root.isEmpty()) return;

            // Load profile
            if (root.get("profile") instanceof Map<?, ?> pMap) {
                this.profile = mapToProfile((Map<String, Object>) pMap);
            }

            // Load activities
            if (root.get("activities") instanceof List<?> list) {
                this.activities.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.activities.add(mapToActivity((Map<String, Object>) m));
                    }
                }
            }

            // Load measurements
            if (root.get("measurements") instanceof List<?> list) {
                this.measurements.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.measurements.add(mapToMeasurement((Map<String, Object>) m));
                    }
                }
            }

            // Load vitals
            if (root.get("vitals") instanceof List<?> list) {
                this.vitals.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.vitals.add(mapToVital((Map<String, Object>) m));
                    }
                }
            }

            // Load nutrition
            if (root.get("nutritionLogs") instanceof List<?> list) {
                this.nutritionLogs.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.nutritionLogs.add(mapToNutrition((Map<String, Object>) m));
                    }
                }
            }

            // Load sleep
            if (root.get("sleepSessions") instanceof List<?> list) {
                this.sleepSessions.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.sleepSessions.add(mapToSleep((Map<String, Object>) m));
                    }
                }
            }

            // Load cycle
            if (root.get("cycleLogs") instanceof List<?> list) {
                this.cycleLogs.clear();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        this.cycleLogs.add(mapToCycle((Map<String, Object>) m));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load existing data: " + e.getMessage());
        }
    }

    // Helper mappers
    private UserProfile mapToProfile(Map<String, Object> m) {
        UserProfile p = new UserProfile();
        if (m.containsKey("id")) p.setId((String) m.get("id"));
        if (m.containsKey("name")) p.setName((String) m.get("name"));
        if (m.containsKey("age")) p.setAge(((Number) m.get("age")).intValue());
        if (m.containsKey("gender")) p.setGender((String) m.get("gender"));
        if (m.containsKey("heightCm")) p.setHeightCm(((Number) m.get("heightCm")).doubleValue());
        if (m.containsKey("targetWeightKg")) p.setTargetWeightKg(((Number) m.get("targetWeightKg")).doubleValue());
        if (m.containsKey("dailyStepGoal")) p.setDailyStepGoal(((Number) m.get("dailyStepGoal")).intValue());
        if (m.containsKey("dailyCalorieGoal")) p.setDailyCalorieGoal(((Number) m.get("dailyCalorieGoal")).intValue());
        if (m.containsKey("dailyWaterGoalMl")) p.setDailyWaterGoalMl(((Number) m.get("dailyWaterGoalMl")).intValue());
        if (m.containsKey("sleepTargetHours")) p.setSleepTargetHours(((Number) m.get("sleepTargetHours")).doubleValue());
        if (m.containsKey("cycleLengthDays")) p.setCycleLengthDays(((Number) m.get("cycleLengthDays")).intValue());
        if (m.containsKey("periodLengthDays")) p.setPeriodLengthDays(((Number) m.get("periodLengthDays")).intValue());
        return p;
    }

    private Activity mapToActivity(Map<String, Object> m) {
        Activity a = new Activity();
        if (m.containsKey("id")) a.setId((String) m.get("id"));
        if (m.containsKey("name")) a.setName((String) m.get("name"));
        if (m.containsKey("type")) a.setType((String) m.get("type"));
        if (m.containsKey("timestamp")) a.setTimestamp((String) m.get("timestamp"));
        if (m.containsKey("durationMinutes")) a.setDurationMinutes(((Number) m.get("durationMinutes")).doubleValue());
        if (m.containsKey("distanceKm")) a.setDistanceKm(((Number) m.get("distanceKm")).doubleValue());
        if (m.containsKey("caloriesBurned")) a.setCaloriesBurned(((Number) m.get("caloriesBurned")).intValue());
        if (m.containsKey("steps")) a.setSteps(((Number) m.get("steps")).intValue());
        if (m.containsKey("avgHeartRate")) a.setAvgHeartRate(((Number) m.get("avgHeartRate")).intValue());
        if (m.containsKey("maxHeartRate")) a.setMaxHeartRate(((Number) m.get("maxHeartRate")).intValue());
        if (m.containsKey("intensity")) a.setIntensity((String) m.get("intensity"));
        if (m.containsKey("notes")) a.setNotes((String) m.get("notes"));
        return a;
    }

    private BodyMeasurement mapToMeasurement(Map<String, Object> m) {
        BodyMeasurement b = new BodyMeasurement();
        if (m.containsKey("id")) b.setId((String) m.get("id"));
        if (m.containsKey("timestamp")) b.setTimestamp((String) m.get("timestamp"));
        if (m.containsKey("weightKg")) b.setWeightKg(((Number) m.get("weightKg")).doubleValue());
        if (m.containsKey("heightCm")) b.setHeightCm(((Number) m.get("heightCm")).doubleValue());
        if (m.containsKey("bmi")) b.setBmi(((Number) m.get("bmi")).doubleValue());
        if (m.containsKey("bmiCategory")) b.setBmiCategory((String) m.get("bmiCategory"));
        if (m.containsKey("bodyFatPercent")) b.setBodyFatPercent(((Number) m.get("bodyFatPercent")).doubleValue());
        if (m.containsKey("muscleMassKg")) b.setMuscleMassKg(((Number) m.get("muscleMassKg")).doubleValue());
        if (m.containsKey("chestCm")) b.setChestCm(((Number) m.get("chestCm")).doubleValue());
        if (m.containsKey("waistCm")) b.setWaistCm(((Number) m.get("waistCm")).doubleValue());
        if (m.containsKey("hipsCm")) b.setHipsCm(((Number) m.get("hipsCm")).doubleValue());
        if (m.containsKey("bicepsCm")) b.setBicepsCm(((Number) m.get("bicepsCm")).doubleValue());
        if (m.containsKey("thighsCm")) b.setThighsCm(((Number) m.get("thighsCm")).doubleValue());
        if (m.containsKey("waistToHipRatio")) b.setWaistToHipRatio(((Number) m.get("waistToHipRatio")).doubleValue());
        if (m.containsKey("notes")) b.setNotes((String) m.get("notes"));
        b.recalculateMetrics();
        return b;
    }

    private Vital mapToVital(Map<String, Object> m) {
        Vital v = new Vital();
        if (m.containsKey("id")) v.setId((String) m.get("id"));
        if (m.containsKey("timestamp")) v.setTimestamp((String) m.get("timestamp"));
        if (m.containsKey("heartRateBpm")) v.setHeartRateBpm(((Number) m.get("heartRateBpm")).intValue());
        if (m.containsKey("restingHeartRateBpm")) v.setRestingHeartRateBpm(((Number) m.get("restingHeartRateBpm")).intValue());
        if (m.containsKey("systolicBp")) v.setSystolicBp(((Number) m.get("systolicBp")).intValue());
        if (m.containsKey("diastolicBp")) v.setDiastolicBp(((Number) m.get("diastolicBp")).intValue());
        if (m.containsKey("spO2Percent")) v.setSpO2Percent(((Number) m.get("spO2Percent")).doubleValue());
        if (m.containsKey("bloodGlucoseMgDl")) v.setBloodGlucoseMgDl(((Number) m.get("bloodGlucoseMgDl")).doubleValue());
        if (m.containsKey("bodyTempC")) v.setBodyTempC(((Number) m.get("bodyTempC")).doubleValue());
        if (m.containsKey("notes")) v.setNotes((String) m.get("notes"));
        v.evaluateCategories();
        return v;
    }

    private NutritionLog mapToNutrition(Map<String, Object> m) {
        NutritionLog n = new NutritionLog();
        if (m.containsKey("id")) n.setId((String) m.get("id"));
        if (m.containsKey("timestamp")) n.setTimestamp((String) m.get("timestamp"));
        if (m.containsKey("mealType")) n.setMealType((String) m.get("mealType"));
        if (m.containsKey("foodName")) n.setFoodName((String) m.get("foodName"));
        if (m.containsKey("calories")) n.setCalories(((Number) m.get("calories")).intValue());
        if (m.containsKey("proteinGrams")) n.setProteinGrams(((Number) m.get("proteinGrams")).doubleValue());
        if (m.containsKey("carbsGrams")) n.setCarbsGrams(((Number) m.get("carbsGrams")).doubleValue());
        if (m.containsKey("fatGrams")) n.setFatGrams(((Number) m.get("fatGrams")).doubleValue());
        if (m.containsKey("fiberGrams")) n.setFiberGrams(((Number) m.get("fiberGrams")).doubleValue());
        if (m.containsKey("waterMl")) n.setWaterMl(((Number) m.get("waterMl")).intValue());
        if (m.containsKey("notes")) n.setNotes((String) m.get("notes"));
        return n;
    }

    private SleepSession mapToSleep(Map<String, Object> m) {
        SleepSession s = new SleepSession();
        if (m.containsKey("id")) s.setId((String) m.get("id"));
        if (m.containsKey("sleepStart")) s.setSleepStart((String) m.get("sleepStart"));
        if (m.containsKey("sleepEnd")) s.setSleepEnd((String) m.get("sleepEnd"));
        if (m.containsKey("totalMinutes")) s.setTotalMinutes(((Number) m.get("totalMinutes")).intValue());
        if (m.containsKey("deepMinutes")) s.setDeepMinutes(((Number) m.get("deepMinutes")).intValue());
        if (m.containsKey("lightMinutes")) s.setLightMinutes(((Number) m.get("lightMinutes")).intValue());
        if (m.containsKey("remMinutes")) s.setRemMinutes(((Number) m.get("remMinutes")).intValue());
        if (m.containsKey("awakeMinutes")) s.setAwakeMinutes(((Number) m.get("awakeMinutes")).intValue());
        if (m.containsKey("notes")) s.setNotes((String) m.get("notes"));
        s.calculateSleepScore();
        return s;
    }

    @SuppressWarnings("unchecked")
    private CycleLog mapToCycle(Map<String, Object> m) {
        CycleLog c = new CycleLog();
        if (m.containsKey("id")) c.setId((String) m.get("id"));
        if (m.containsKey("logDate")) c.setLogDate((String) m.get("logDate"));
        if (m.containsKey("cycleStartDate")) c.setCycleStartDate((String) m.get("cycleStartDate"));
        if (m.containsKey("cycleDay")) c.setCycleDay(((Number) m.get("cycleDay")).intValue());
        if (m.containsKey("phase")) c.setPhase((String) m.get("phase"));
        if (m.containsKey("flow")) c.setFlow((String) m.get("flow"));
        if (m.containsKey("mood")) c.setMood((String) m.get("mood"));
        if (m.containsKey("cervicalMucus")) c.setCervicalMucus((String) m.get("cervicalMucus"));
        if (m.containsKey("basalBodyTempC")) c.setBasalBodyTempC(((Number) m.get("basalBodyTempC")).doubleValue());
        if (m.containsKey("isFertileWindow")) c.setFertileWindow((Boolean) m.get("isFertileWindow"));
        if (m.containsKey("isOvulationDay")) c.setOvulationDay((Boolean) m.get("isOvulationDay"));
        if (m.containsKey("predictedNextPeriod")) c.setPredictedNextPeriod((String) m.get("predictedNextPeriod"));
        if (m.containsKey("predictedOvulationDate")) c.setPredictedOvulationDate((String) m.get("predictedOvulationDate"));
        if (m.containsKey("notes")) c.setNotes((String) m.get("notes"));
        if (m.get("symptoms") instanceof List<?> symList) {
            List<String> sl = new ArrayList<>();
            for (Object o : symList) if (o != null) sl.add(o.toString());
            c.setSymptoms(sl);
        }
        return c;
    }

    // ==========================================
    // GETTERS & DATA ACCESS
    // ==========================================
    public UserProfile getProfile() { return profile; }
    public synchronized void updateProfile(UserProfile p) {
        this.profile = p;
        saveToFile();
    }

    public List<Activity> getActivities() { return activities; }
    public synchronized void addActivity(Activity a) {
        activities.add(0, a);
        saveToFile();
    }
    public synchronized boolean deleteActivity(String id) {
        boolean removed = activities.removeIf(a -> a.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public List<BodyMeasurement> getMeasurements() { return measurements; }
    public synchronized void addMeasurement(BodyMeasurement m) {
        m.recalculateMetrics();
        measurements.add(0, m);
        saveToFile();
    }
    public synchronized boolean deleteMeasurement(String id) {
        boolean removed = measurements.removeIf(m -> m.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public List<Vital> getVitals() { return vitals; }
    public synchronized void addVital(Vital v) {
        v.evaluateCategories();
        vitals.add(0, v);
        saveToFile();
    }
    public synchronized boolean deleteVital(String id) {
        boolean removed = vitals.removeIf(v -> v.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public List<NutritionLog> getNutritionLogs() { return nutritionLogs; }
    public synchronized void addNutritionLog(NutritionLog n) {
        nutritionLogs.add(0, n);
        saveToFile();
    }
    public synchronized boolean deleteNutritionLog(String id) {
        boolean removed = nutritionLogs.removeIf(n -> n.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public List<SleepSession> getSleepSessions() { return sleepSessions; }
    public synchronized void addSleepSession(SleepSession s) {
        s.calculateSleepScore();
        sleepSessions.add(0, s);
        saveToFile();
    }
    public synchronized boolean deleteSleepSession(String id) {
        boolean removed = sleepSessions.removeIf(s -> s.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public List<CycleLog> getCycleLogs() { return cycleLogs; }
    public synchronized void addCycleLog(CycleLog c) {
        cycleLogs.add(0, c);
        saveToFile();
    }
    public synchronized boolean deleteCycleLog(String id) {
        boolean removed = cycleLogs.removeIf(c -> c.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    /**
     * Compute comprehensive 3D Dashboard Summary
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        // Profile & Goals
        summary.put("profile", profile);

        // Daily steps & calories from today's activities
        String todayStr = LocalDate.now().toString();
        int todaySteps = 0;
        int todayCaloriesBurned = 0;
        double todayDuration = 0;
        for (Activity a : activities) {
            if (a.getTimestamp() != null && a.getTimestamp().startsWith(todayStr)) {
                todaySteps += a.getSteps();
                todayCaloriesBurned += a.getCaloriesBurned();
                todayDuration += a.getDurationMinutes();
            }
        }
        summary.put("todaySteps", todaySteps);
        summary.put("todayCaloriesBurned", todayCaloriesBurned);
        summary.put("todayActiveMinutes", todayDuration);

        // Nutrition totals for today
        int todayCaloriesConsumed = 0;
        double todayProtein = 0;
        double todayCarbs = 0;
        double todayFat = 0;
        int todayWaterMl = 0;
        for (NutritionLog n : nutritionLogs) {
            if (n.getTimestamp() != null && n.getTimestamp().startsWith(todayStr)) {
                todayCaloriesConsumed += n.getCalories();
                todayProtein += n.getProteinGrams();
                todayCarbs += n.getCarbsGrams();
                todayFat += n.getFatGrams();
                todayWaterMl += n.getWaterMl();
            }
        }
        Map<String, Object> nutritionSummary = new LinkedHashMap<>();
        nutritionSummary.put("calories", todayCaloriesConsumed);
        nutritionSummary.put("protein", Math.round(todayProtein * 10.0) / 10.0);
        nutritionSummary.put("carbs", Math.round(todayCarbs * 10.0) / 10.0);
        nutritionSummary.put("fat", Math.round(todayFat * 10.0) / 10.0);
        nutritionSummary.put("waterMl", todayWaterMl);
        summary.put("todayNutrition", nutritionSummary);

        // Latest body measurement
        BodyMeasurement latestMeasure = measurements.isEmpty() ? null : measurements.get(0);
        summary.put("latestMeasurement", latestMeasure);

        // Latest vitals
        Vital latestVital = vitals.isEmpty() ? null : vitals.get(0);
        summary.put("latestVital", latestVital);

        // Latest sleep session
        SleepSession latestSleep = sleepSessions.isEmpty() ? null : sleepSessions.get(0);
        summary.put("latestSleep", latestSleep);

        // Menstrual Cycle status
        LocalDate cycleAnchor = LocalDate.now().minusDays(10);
        if (!cycleLogs.isEmpty() && cycleLogs.get(0).getCycleStartDate() != null) {
            try {
                cycleAnchor = LocalDate.parse(cycleLogs.get(0).getCycleStartDate());
            } catch (Exception ignored) {}
        }
        var cycleStatus = CyclePredictor.evaluateCycle(cycleAnchor, profile);
        summary.put("cycleStatus", cycleStatus);

        // Overall Wellness Index
        int sleepScore = latestSleep != null ? latestSleep.getSleepScore() : 80;
        int restingHr = latestVital != null ? latestVital.getRestingHeartRateBpm() : 62;
        double spO2 = latestVital != null ? latestVital.getSpO2Percent() : 98.5;
        int wellnessScore = HealthCalculator.calculateWellnessScore(
            todaySteps, profile.getDailyStepGoal(),
            sleepScore,
            todayCaloriesConsumed, todayCaloriesBurned, profile.getDailyCalorieGoal(),
            restingHr, spO2
        );
        summary.put("wellnessScore", wellnessScore);

        return summary;
    }
}
