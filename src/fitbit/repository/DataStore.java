package fitbit.repository;

import fitbit.analytics.CyclePredictor;
import fitbit.analytics.HealthCalculator;
import fitbit.model.*;
import fitbit.server.JsonUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Enterprise Multi-User DataStore supporting isolated user profiles,
 * multi-user activity tracking, and JSON & SQLite persistence.
 */
public class DataStore {
    private static final String DATA_FILE = "data/fitness_data.json";

    private final Map<String, UserProfile> users = new ConcurrentHashMap<>();
    private String activeUserId = "user_1";

    private final List<Activity> activities = new CopyOnWriteArrayList<>();
    private final List<BodyMeasurement> measurements = new CopyOnWriteArrayList<>();
    private final List<Vital> vitals = new CopyOnWriteArrayList<>();
    private final List<NutritionLog> nutritionLogs = new CopyOnWriteArrayList<>();
    private final List<SleepSession> sleepSessions = new CopyOnWriteArrayList<>();
    private final List<CycleLog> cycleLogs = new CopyOnWriteArrayList<>();

    public DataStore() {
        loadFromFile();
        if (users.size() < 3 || !users.containsKey("user_1") || !users.containsKey("user_2")) {
            seedDemoData();
            saveToFile();
        }
    }

    // ==========================================
    // MULTI-USER DEMO SEEDING
    // ==========================================
    public synchronized void seedDemoData() {
        users.clear();
        activities.clear();
        measurements.clear();
        vitals.clear();
        nutritionLogs.clear();
        sleepSessions.clear();
        cycleLogs.clear();

        LocalDate today = LocalDate.now();

        // -------------------------------------------------------------
        // USER 1: ALEX MORGAN (Runner, Cycle tracking, Endurance)
        // -------------------------------------------------------------
        UserProfile u1 = new UserProfile(
            "user_1", "Alex Morgan", "alex.morgan@fitbit.app", "#1a73e8",
            28, "FEMALE", 168.0, 60.0,
            10000, 600, 2500, 8.0, 28, 5
        );
        users.put(u1.getId(), u1);

        activities.add(new Activity("user_1", "Morning Sunrise 5K", "RUNNING", today.minusDays(5).toString() + "T07:15:00", 32.0, 5.2, 380, 5800, 142, "HIGH", "Great pacing and cool weather"));
        activities.add(new Activity("user_1", "Power Vinyasa Flow", "YOGA", today.minusDays(4).toString() + "T18:00:00", 45.0, 0.0, 190, 1200, 105, "MODERATE", "Focus on core balance"));
        activities.add(new Activity("user_1", "HIIT Sprint Intervals", "HIIT", today.minusDays(3).toString() + "T08:00:00", 30.0, 3.1, 340, 4200, 158, "EXTREME", "Tabata protocol"));
        activities.add(new Activity("user_1", "Evening Recovery Walk", "WALKING", today.minusDays(2).toString() + "T19:30:00", 40.0, 3.8, 180, 4900, 98, "LOW", "Gentle walk"));
        activities.add(new Activity("user_1", "Full Body Circuit", "STRENGTH", today.minusDays(1).toString() + "T17:30:00", 50.0, 0.0, 310, 2100, 128, "HIGH", "Squats, deadlifts"));
        activities.add(new Activity("user_1", "Outdoor Road Cycling", "CYCLING", today.toString() + "T09:00:00", 45.0, 16.5, 420, 6800, 138, "HIGH", "Hilly circuit"));

        measurements.add(new BodyMeasurement("user_1", today.minusDays(14).toString(), 63.8, 168.0, 22.8, 46.2, 88.0, 71.5, 96.0, 27.5, 54.0, "Baseline"));
        measurements.add(new BodyMeasurement("user_1", today.minusDays(7).toString(), 63.1, 168.0, 22.4, 46.5, 87.5, 70.8, 95.5, 27.8, 53.6, "Week 1 progress"));
        measurements.add(new BodyMeasurement("user_1", today.toString(), 62.4, 168.0, 21.9, 46.8, 87.0, 69.8, 95.0, 28.0, 53.2, "Waist down 1.7cm"));

        vitals.add(new Vital("user_1", today.minusDays(2).toString() + "T08:00:00", 68, 60, 116, 75, 98.5, 94.0, 36.5, "Rest day vitals"));
        vitals.add(new Vital("user_1", today.minusDays(1).toString() + "T08:00:00", 70, 61, 119, 78, 99.0, 90.0, 36.6, "Restored"));
        vitals.add(new Vital("user_1", today.toString() + "T08:30:00", 65, 59, 115, 74, 99.5, 89.0, 36.7, "Morning baseline"));

        nutritionLogs.add(new NutritionLog("user_1", today.toString() + "T08:00:00", "BREAKFAST", "Steel Cut Oats with Blueberries & Chia", 410, 24.0, 55.0, 8.0, 9.0, 500, "Clean energy"));
        nutritionLogs.add(new NutritionLog("user_1", today.toString() + "T12:45:00", "LUNCH", "Grilled Lemon Salmon & Quinoa", 580, 42.0, 38.0, 18.0, 6.0, 600, "High omega-3"));
        nutritionLogs.add(new NutritionLog("user_1", today.toString() + "T16:00:00", "SNACK", "Greek Yogurt with Almonds", 220, 18.0, 16.0, 7.0, 2.0, 400, "Afternoon boost"));

        sleepSessions.add(new SleepSession("user_1", today.minusDays(2).toString() + "T23:30:00", today.minusDays(1).toString() + "T07:00:00", 95, 230, 90, 25, "Restful"));
        sleepSessions.add(new SleepSession("user_1", today.minusDays(1).toString() + "T22:45:00", today.toString() + "T07:15:00", 115, 260, 105, 15, "Deep recovery"));

        LocalDate cycleAnchor = today.minusDays(10);
        cycleLogs.add(new CycleLog("user_1", cycleAnchor.toString(), cycleAnchor.toString(), 1, "MENSTRUAL", "MEDIUM",
            List.of("Mild Cramps"), "Calm", "STICKY", 36.4, false, false, cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Period start"));
        cycleLogs.add(new CycleLog("user_1", today.toString(), cycleAnchor.toString(), 11, "FOLLICULAR", "NONE",
            List.of("High Energy"), "Happy", "EGG_WHITE", 36.6, true, false, cycleAnchor.plusDays(28).toString(), cycleAnchor.plusDays(14).toString(), "Energy peaking"));

        // -------------------------------------------------------------
        // USER 2: DAVID CHEN (Male, 34, Strength Training & Muscle Mass)
        // -------------------------------------------------------------
        UserProfile u2 = new UserProfile(
            "user_2", "David Chen", "david.chen@fitbit.app", "#00875a",
            34, "MALE", 182.0, 80.0,
            8000, 750, 3200, 7.5, 0, 0
        );
        users.put(u2.getId(), u2);

        activities.add(new Activity("user_2", "Heavy Push Day (Chest & Triceps)", "STRENGTH", today.minusDays(2).toString() + "T18:00:00", 60.0, 0.0, 440, 2500, 136, "HIGH", "Bench press 95kg, dips"));
        activities.add(new Activity("user_2", "Leg Day Hypertrophy", "STRENGTH", today.minusDays(1).toString() + "T17:45:00", 65.0, 0.0, 520, 3100, 148, "EXTREME", "Squats 120kg, Romanian deadlifts"));
        activities.add(new Activity("user_2", "Incline Treadmill Walk", "WALKING", today.toString() + "T07:30:00", 35.0, 3.2, 260, 4100, 115, "MODERATE", "Incline 8.0, fat burn"));

        measurements.add(new BodyMeasurement("user_2", today.minusDays(10).toString(), 83.2, 182.0, 17.5, 68.5, 106.0, 84.0, 102.0, 38.5, 60.0, "Bulking baseline"));
        measurements.add(new BodyMeasurement("user_2", today.toString(), 82.6, 182.0, 16.9, 68.8, 106.5, 83.0, 101.5, 39.0, 59.8, "Lean bulk progress"));

        vitals.add(new Vital("user_2", today.toString() + "T08:00:00", 62, 56, 122, 78, 99.0, 95.0, 36.6, "Healthy resting BP"));

        nutritionLogs.add(new NutritionLog("user_2", today.toString() + "T08:00:00", "BREAKFAST", "4 Whole Eggs, Sourdough & Avocado", 540, 34.0, 42.0, 22.0, 5.0, 600, "High protein breakfast"));
        nutritionLogs.add(new NutritionLog("user_2", today.toString() + "T13:00:00", "LUNCH", "Grilled Sirloin Steak, Sweet Potato & Broccoli", 720, 58.0, 65.0, 24.0, 8.0, 800, "Anabolic recovery meal"));

        sleepSessions.add(new SleepSession("user_2", today.minusDays(1).toString() + "T23:00:00", today.toString() + "T06:30:00", 100, 230, 85, 20, "Solid recovery"));

        // -------------------------------------------------------------
        // USER 3: ELENA ROSTOVA (Female, 25, Yoga & Mindfulness)
        // -------------------------------------------------------------
        UserProfile u3 = new UserProfile(
            "user_3", "Elena Rostova", "elena.r@fitbit.app", "#9334e6",
            25, "FEMALE", 164.0, 54.0,
            12000, 500, 2400, 8.5, 29, 4
        );
        users.put(u3.getId(), u3);

        activities.add(new Activity("user_3", "Morning Kundalini Yoga", "YOGA", today.toString() + "T06:30:00", 45.0, 0.0, 170, 1500, 98, "MODERATE", "Breathwork and flexibility"));
        activities.add(new Activity("user_3", "Brisk Campus Walk", "WALKING", today.toString() + "T11:30:00", 40.0, 3.5, 160, 4800, 102, "LOW", "Between classes"));

        measurements.add(new BodyMeasurement("user_3", today.toString(), 54.5, 164.0, 20.2, 41.5, 84.0, 65.0, 91.0, 24.5, 49.0, "Healthy baseline"));
        vitals.add(new Vital("user_3", today.toString() + "T07:30:00", 64, 58, 112, 72, 99.5, 88.0, 36.5, "Optimal calmness"));
        sleepSessions.add(new SleepSession("user_3", today.minusDays(1).toString() + "T22:30:00", today.toString() + "T07:00:00", 125, 270, 110, 10, "Exceptional deep sleep"));

        this.activeUserId = "user_1";
    }

    // ==========================================
    // MULTI-USER MANAGEMENT
    // ==========================================
    public List<UserProfile> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public UserProfile getUser(String userId) {
        return users.get(userId);
    }

    public UserProfile getActiveUser() {
        UserProfile p = users.get(activeUserId);
        if (p == null) {
            p = users.values().stream().findFirst().orElseGet(() -> {
                UserProfile fallback = new UserProfile();
                users.put(fallback.getId(), fallback);
                return fallback;
            });
            this.activeUserId = p.getId();
        }
        return p;
    }

    public synchronized boolean setActiveUserId(String userId) {
        if (userId != null && users.containsKey(userId)) {
            this.activeUserId = userId;
            saveToFile();
            return true;
        }
        return false;
    }

    public synchronized UserProfile createUser(UserProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Profile cannot be null");
        if (profile.getId() == null || profile.getId().isBlank()) {
            profile.setId("user_" + UUID.randomUUID().toString().substring(0, 8));
        }
        users.put(profile.getId(), profile);
        this.activeUserId = profile.getId();
        saveToFile();
        return profile;
    }

    public synchronized void updateUser(UserProfile profile) {
        if (profile != null && profile.getId() != null) {
            users.put(profile.getId(), profile);
            saveToFile();
        }
    }

    public synchronized boolean deleteUser(String userId) {
        if (users.size() <= 1) {
            return false; // prevent deleting only user
        }
        boolean removed = users.remove(userId) != null;
        if (removed) {
            // Remove user's data
            activities.removeIf(a -> userId.equals(a.getUserId()));
            measurements.removeIf(m -> userId.equals(m.getUserId()));
            vitals.removeIf(v -> userId.equals(v.getUserId()));
            nutritionLogs.removeIf(n -> userId.equals(n.getUserId()));
            sleepSessions.removeIf(s -> userId.equals(s.getUserId()));
            cycleLogs.removeIf(c -> userId.equals(c.getUserId()));

            if (userId.equals(activeUserId)) {
                this.activeUserId = users.keySet().iterator().next();
            }
            saveToFile();
        }
        return removed;
    }

    // ==========================================
    // MULTI-USER DATA ACCESS (ISOLATED BY USER ID)
    // ==========================================
    public List<Activity> getActivitiesForUser(String userId) {
        return activities.stream()
            .filter(a -> userId.equals(a.getUserId()))
            .collect(Collectors.toList());
    }

    public List<BodyMeasurement> getMeasurementsForUser(String userId) {
        return measurements.stream()
            .filter(m -> userId.equals(m.getUserId()))
            .collect(Collectors.toList());
    }

    public List<Vital> getVitalsForUser(String userId) {
        return vitals.stream()
            .filter(v -> userId.equals(v.getUserId()))
            .collect(Collectors.toList());
    }

    public List<NutritionLog> getNutritionLogsForUser(String userId) {
        return nutritionLogs.stream()
            .filter(n -> userId.equals(n.getUserId()))
            .collect(Collectors.toList());
    }

    public List<SleepSession> getSleepSessionsForUser(String userId) {
        return sleepSessions.stream()
            .filter(s -> userId.equals(s.getUserId()))
            .collect(Collectors.toList());
    }

    public List<CycleLog> getCycleLogsForUser(String userId) {
        return cycleLogs.stream()
            .filter(c -> userId.equals(c.getUserId()))
            .collect(Collectors.toList());
    }

    // Mutators (auto-associate with active user if needed)
    public synchronized void addActivity(Activity a) {
        if (a.getUserId() == null || a.getUserId().isBlank()) {
            a.setUserId(activeUserId);
        }
        activities.add(0, a);
        saveToFile();
    }

    public synchronized boolean deleteActivity(String id) {
        boolean removed = activities.removeIf(a -> a.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public synchronized void addMeasurement(BodyMeasurement m) {
        if (m.getUserId() == null || m.getUserId().isBlank()) {
            m.setUserId(activeUserId);
        }
        m.recalculateMetrics();
        measurements.add(0, m);
        saveToFile();
    }

    public synchronized boolean deleteMeasurement(String id) {
        boolean removed = measurements.removeIf(m -> m.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public synchronized void addVital(Vital v) {
        if (v.getUserId() == null || v.getUserId().isBlank()) {
            v.setUserId(activeUserId);
        }
        v.evaluateCategories();
        vitals.add(0, v);
        saveToFile();
    }

    public synchronized boolean deleteVital(String id) {
        boolean removed = vitals.removeIf(v -> v.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public synchronized void addNutritionLog(NutritionLog n) {
        if (n.getUserId() == null || n.getUserId().isBlank()) {
            n.setUserId(activeUserId);
        }
        nutritionLogs.add(0, n);
        saveToFile();
    }

    public synchronized boolean deleteNutritionLog(String id) {
        boolean removed = nutritionLogs.removeIf(n -> n.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public synchronized void addSleepSession(SleepSession s) {
        if (s.getUserId() == null || s.getUserId().isBlank()) {
            s.setUserId(activeUserId);
        }
        s.calculateSleepScore();
        sleepSessions.add(0, s);
        saveToFile();
    }

    public synchronized boolean deleteSleepSession(String id) {
        boolean removed = sleepSessions.removeIf(s -> s.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    public synchronized void addCycleLog(CycleLog c) {
        if (c.getUserId() == null || c.getUserId().isBlank()) {
            c.setUserId(activeUserId);
        }
        cycleLogs.add(0, c);
        saveToFile();
    }

    public synchronized boolean deleteCycleLog(String id) {
        boolean removed = cycleLogs.removeIf(c -> c.getId().equals(id));
        if (removed) saveToFile();
        return removed;
    }

    // ==========================================
    // MULTI-USER DASHBOARD SUMMARY
    // ==========================================
    public Map<String, Object> getDashboardSummary() {
        return getDashboardSummaryForUser(activeUserId);
    }

    public Map<String, Object> getDashboardSummaryForUser(String userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        UserProfile profile = getUser(userId);
        if (profile == null) profile = getActiveUser();

        summary.put("profile", profile);
        summary.put("activeUserId", profile.getId());

        String todayStr = LocalDate.now().toString();

        // 1. Today's Activities for this user
        int todaySteps = 0;
        int todayCaloriesBurned = 0;
        double todayDuration = 0;
        List<Activity> userActs = getActivitiesForUser(profile.getId());
        for (Activity a : userActs) {
            if (a.getTimestamp() != null && a.getTimestamp().startsWith(todayStr)) {
                todaySteps += a.getSteps();
                todayCaloriesBurned += a.getCaloriesBurned();
                todayDuration += a.getDurationMinutes();
            }
        }
        summary.put("todaySteps", todaySteps);
        summary.put("todayCaloriesBurned", todayCaloriesBurned);
        summary.put("todayActiveMinutes", todayDuration);

        // 2. Today's Nutrition for this user
        int todayCaloriesConsumed = 0;
        double todayProtein = 0;
        double todayCarbs = 0;
        double todayFat = 0;
        int todayWaterMl = 0;
        List<NutritionLog> userNutr = getNutritionLogsForUser(profile.getId());
        for (NutritionLog n : userNutr) {
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

        // 3. Latest records for this user
        List<BodyMeasurement> userBm = getMeasurementsForUser(profile.getId());
        summary.put("latestMeasurement", userBm.isEmpty() ? null : userBm.get(0));

        List<Vital> userVitals = getVitalsForUser(profile.getId());
        summary.put("latestVital", userVitals.isEmpty() ? null : userVitals.get(0));

        List<SleepSession> userSleep = getSleepSessionsForUser(profile.getId());
        summary.put("latestSleep", userSleep.isEmpty() ? null : userSleep.get(0));

        // 4. Cycle status for female users
        List<CycleLog> userCycle = getCycleLogsForUser(profile.getId());
        LocalDate cycleAnchor = LocalDate.now().minusDays(10);
        if (!userCycle.isEmpty() && userCycle.get(0).getCycleStartDate() != null) {
            try {
                cycleAnchor = LocalDate.parse(userCycle.get(0).getCycleStartDate());
            } catch (Exception ignored) {}
        }
        if ("FEMALE".equalsIgnoreCase(profile.getGender())) {
            summary.put("cycleStatus", CyclePredictor.evaluateCycle(cycleAnchor, profile));
        } else {
            summary.put("cycleStatus", null);
        }

        // 5. Wellness score
        int sleepScore = !userSleep.isEmpty() ? userSleep.get(0).getSleepScore() : 80;
        int restingHr = !userVitals.isEmpty() ? userVitals.get(0).getRestingHeartRateBpm() : 62;
        double spO2 = !userVitals.isEmpty() ? userVitals.get(0).getSpO2Percent() : 98.5;
        int wellnessScore = HealthCalculator.calculateWellnessScore(
            todaySteps, profile.getDailyStepGoal(),
            sleepScore,
            todayCaloriesConsumed, todayCaloriesBurned, profile.getDailyCalorieGoal(),
            restingHr, spO2
        );
        summary.put("wellnessScore", wellnessScore);

        return summary;
    }

    // ==========================================
    // PERSISTENCE (JSON FILE ENGINE)
    // ==========================================
    public synchronized void saveToFile() {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("activeUserId", activeUserId);
            root.put("users", users.values());
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

            if (root.containsKey("activeUserId")) {
                this.activeUserId = (String) root.get("activeUserId");
            }

            // Load users list
            if (root.get("users") instanceof List<?> userList) {
                this.users.clear();
                for (Object item : userList) {
                    if (item instanceof Map<?, ?> m) {
                        UserProfile p = mapToProfile((Map<String, Object>) m);
                        this.users.put(p.getId(), p);
                    }
                }
            } else if (root.get("profile") instanceof Map<?, ?> pMap) {
                // Backward compatibility for single user
                UserProfile p = mapToProfile((Map<String, Object>) pMap);
                this.users.put(p.getId(), p);
                this.activeUserId = p.getId();
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

    private UserProfile mapToProfile(Map<String, Object> m) {
        UserProfile p = new UserProfile();
        if (m.containsKey("id")) p.setId((String) m.get("id"));
        if (m.containsKey("name")) p.setName((String) m.get("name"));
        if (m.containsKey("email")) p.setEmail((String) m.get("email"));
        if (m.containsKey("avatarColor")) p.setAvatarColor((String) m.get("avatarColor"));
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
        if (m.containsKey("userId")) a.setUserId((String) m.get("userId"));
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
        if (m.containsKey("userId")) b.setUserId((String) m.get("userId"));
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
        if (m.containsKey("notes")) b.setNotes((String) m.get("notes"));
        b.recalculateMetrics();
        return b;
    }

    private Vital mapToVital(Map<String, Object> m) {
        Vital v = new Vital();
        if (m.containsKey("id")) v.setId((String) m.get("id"));
        if (m.containsKey("userId")) v.setUserId((String) m.get("userId"));
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
        if (m.containsKey("userId")) n.setUserId((String) m.get("userId"));
        if (m.containsKey("timestamp")) n.setTimestamp((String) m.get("timestamp"));
        if (m.containsKey("mealType")) n.setMealType((String) m.get("mealType"));
        if (m.containsKey("foodName")) n.setFoodName((String) m.get("foodName"));
        if (m.containsKey("calories")) n.setCalories(((Number) m.get("calories")).intValue());
        if (m.containsKey("proteinGrams")) n.setProteinGrams(((Number) m.get("proteinGrams")).doubleValue());
        if (m.containsKey("carbsGrams")) n.setCarbsGrams(((Number) m.get("carbsGrams")).doubleValue());
        if (m.containsKey("fatGrams")) n.setFatGrams(((Number) m.get("fatGrams")).doubleValue());
        if (m.containsKey("waterMl")) n.setWaterMl(((Number) m.get("waterMl")).intValue());
        if (m.containsKey("notes")) n.setNotes((String) m.get("notes"));
        return n;
    }

    private SleepSession mapToSleep(Map<String, Object> m) {
        SleepSession s = new SleepSession();
        if (m.containsKey("id")) s.setId((String) m.get("id"));
        if (m.containsKey("userId")) s.setUserId((String) m.get("userId"));
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
        if (m.containsKey("userId")) c.setUserId((String) m.get("userId"));
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
}
