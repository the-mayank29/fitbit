package fitbit.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fitbit.analytics.CyclePredictor;
import fitbit.model.*;
import fitbit.repository.DataStore;
import fitbit.server.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ApiHandler implements HttpHandler {
    private final DataStore dataStore;

    public ApiHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendResponse(exchange, 204, "");
            return;
        }

        try {
            Map<String, String> queryParams = parseQueryParams(uri.getQuery());
            String targetUser = queryParams.getOrDefault("userId", dataStore.getActiveUser().getId());

            switch (path) {
                // ==========================================
                // MULTI-USER MANAGEMENT ENDPOINTS
                // ==========================================
                case "/api/users" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        Map<String, Object> resp = new LinkedHashMap<>();
                        resp.put("activeUserId", dataStore.getActiveUser().getId());
                        resp.put("users", dataStore.getAllUsers());
                        sendJsonResponse(exchange, 200, resp);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        UserProfile u = new UserProfile();
                        if (m.containsKey("id")) u.setId((String) m.get("id"));
                        if (m.containsKey("name")) u.setName((String) m.get("name"));
                        if (m.containsKey("email")) u.setEmail((String) m.get("email"));
                        if (m.containsKey("avatarColor")) u.setAvatarColor((String) m.get("avatarColor"));
                        if (m.containsKey("age")) u.setAge(((Number) m.get("age")).intValue());
                        if (m.containsKey("gender")) u.setGender((String) m.get("gender"));
                        if (m.containsKey("heightCm")) u.setHeightCm(((Number) m.get("heightCm")).doubleValue());
                        if (m.containsKey("targetWeightKg")) u.setTargetWeightKg(((Number) m.get("targetWeightKg")).doubleValue());
                        if (m.containsKey("dailyStepGoal")) u.setDailyStepGoal(((Number) m.get("dailyStepGoal")).intValue());
                        if (m.containsKey("dailyCalorieGoal")) u.setDailyCalorieGoal(((Number) m.get("dailyCalorieGoal")).intValue());
                        if (m.containsKey("dailyWaterGoalMl")) u.setDailyWaterGoalMl(((Number) m.get("dailyWaterGoalMl")).intValue());
                        UserProfile created = dataStore.createUser(u);
                        sendJsonResponse(exchange, 201, created);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteUser(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true, "message", "User deleted"));
                        } else {
                            sendJsonResponse(exchange, 400, Map.of("error", "Cannot delete user (minimum 1 user required)"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/users/switch" -> {
                    if ("POST".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id == null) {
                            String body = readBody(exchange);
                            Map<String, Object> m = JsonUtil.parseObject(body);
                            id = (String) m.get("id");
                        }
                        if (id != null && dataStore.setActiveUserId(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true, "activeUser", dataStore.getActiveUser()));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "User ID not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }

                // ==========================================
                // DASHBOARD & USER DATA ENDPOINTS
                // ==========================================
                case "/api/dashboard" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getDashboardSummaryForUser(targetUser));
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/profile" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getUser(targetUser));
                    } else if ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> map = JsonUtil.parseObject(body);
                        UserProfile current = dataStore.getUser(targetUser);
                        if (current == null) current = dataStore.getActiveUser();

                        if (map.containsKey("name")) current.setName((String) map.get("name"));
                        if (map.containsKey("email")) current.setEmail((String) map.get("email"));
                        if (map.containsKey("avatarColor")) current.setAvatarColor((String) map.get("avatarColor"));
                        if (map.containsKey("age")) current.setAge(((Number) map.get("age")).intValue());
                        if (map.containsKey("gender")) current.setGender((String) map.get("gender"));
                        if (map.containsKey("heightCm")) current.setHeightCm(((Number) map.get("heightCm")).doubleValue());
                        if (map.containsKey("targetWeightKg")) current.setTargetWeightKg(((Number) map.get("targetWeightKg")).doubleValue());
                        if (map.containsKey("dailyStepGoal")) current.setDailyStepGoal(((Number) map.get("dailyStepGoal")).intValue());
                        if (map.containsKey("dailyCalorieGoal")) current.setDailyCalorieGoal(((Number) map.get("dailyCalorieGoal")).intValue());
                        if (map.containsKey("dailyWaterGoalMl")) current.setDailyWaterGoalMl(((Number) map.get("dailyWaterGoalMl")).intValue());
                        if (map.containsKey("sleepTargetHours")) current.setSleepTargetHours(((Number) map.get("sleepTargetHours")).doubleValue());
                        if (map.containsKey("cycleLengthDays")) current.setCycleLengthDays(((Number) map.get("cycleLengthDays")).intValue());
                        if (map.containsKey("periodLengthDays")) current.setPeriodLengthDays(((Number) map.get("periodLengthDays")).intValue());
                        dataStore.updateUser(current);
                        sendJsonResponse(exchange, 200, current);
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/activities" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getActivitiesForUser(targetUser));
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        Activity a = new Activity();
                        a.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        if (m.containsKey("name")) a.setName((String) m.get("name"));
                        if (m.containsKey("type")) a.setType((String) m.get("type"));
                        if (m.containsKey("timestamp")) {
                            a.setTimestamp((String) m.get("timestamp"));
                        } else {
                            a.setTimestamp(LocalDateTime.now().toString());
                        }
                        if (m.containsKey("durationMinutes")) a.setDurationMinutes(((Number) m.get("durationMinutes")).doubleValue());
                        if (m.containsKey("distanceKm")) a.setDistanceKm(((Number) m.get("distanceKm")).doubleValue());
                        if (m.containsKey("caloriesBurned")) a.setCaloriesBurned(((Number) m.get("caloriesBurned")).intValue());
                        if (m.containsKey("steps")) a.setSteps(((Number) m.get("steps")).intValue());
                        if (m.containsKey("avgHeartRate")) a.setAvgHeartRate(((Number) m.get("avgHeartRate")).intValue());
                        if (m.containsKey("intensity")) a.setIntensity((String) m.get("intensity"));
                        if (m.containsKey("notes")) a.setNotes((String) m.get("notes"));
                        dataStore.addActivity(a);
                        sendJsonResponse(exchange, 201, a);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteActivity(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Activity deleted"));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Activity not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/measurements" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getMeasurementsForUser(targetUser));
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        BodyMeasurement b = new BodyMeasurement();
                        b.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        b.setTimestamp(m.containsKey("timestamp") ? (String) m.get("timestamp") : LocalDate.now().toString());
                        if (m.containsKey("weightKg")) b.setWeightKg(((Number) m.get("weightKg")).doubleValue());
                        if (m.containsKey("heightCm")) b.setHeightCm(((Number) m.get("heightCm")).doubleValue());
                        if (m.containsKey("bodyFatPercent")) b.setBodyFatPercent(((Number) m.get("bodyFatPercent")).doubleValue());
                        if (m.containsKey("muscleMassKg")) b.setMuscleMassKg(((Number) m.get("muscleMassKg")).doubleValue());
                        if (m.containsKey("chestCm")) b.setChestCm(((Number) m.get("chestCm")).doubleValue());
                        if (m.containsKey("waistCm")) b.setWaistCm(((Number) m.get("waistCm")).doubleValue());
                        if (m.containsKey("hipsCm")) b.setHipsCm(((Number) m.get("hipsCm")).doubleValue());
                        if (m.containsKey("bicepsCm")) b.setBicepsCm(((Number) m.get("bicepsCm")).doubleValue());
                        if (m.containsKey("thighsCm")) b.setThighsCm(((Number) m.get("thighsCm")).doubleValue());
                        if (m.containsKey("notes")) b.setNotes((String) m.get("notes"));
                        b.recalculateMetrics();
                        dataStore.addMeasurement(b);
                        sendJsonResponse(exchange, 201, b);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteMeasurement(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Measurement not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/vitals" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getVitalsForUser(targetUser));
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        Vital v = new Vital();
                        v.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        v.setTimestamp(m.containsKey("timestamp") ? (String) m.get("timestamp") : LocalDateTime.now().toString());
                        if (m.containsKey("heartRateBpm")) v.setHeartRateBpm(((Number) m.get("heartRateBpm")).intValue());
                        if (m.containsKey("restingHeartRateBpm")) v.setRestingHeartRateBpm(((Number) m.get("restingHeartRateBpm")).intValue());
                        if (m.containsKey("systolicBp")) v.setSystolicBp(((Number) m.get("systolicBp")).intValue());
                        if (m.containsKey("diastolicBp")) v.setDiastolicBp(((Number) m.get("diastolicBp")).intValue());
                        if (m.containsKey("spO2Percent")) v.setSpO2Percent(((Number) m.get("spO2Percent")).doubleValue());
                        if (m.containsKey("bloodGlucoseMgDl")) v.setBloodGlucoseMgDl(((Number) m.get("bloodGlucoseMgDl")).doubleValue());
                        if (m.containsKey("bodyTempC")) v.setBodyTempC(((Number) m.get("bodyTempC")).doubleValue());
                        if (m.containsKey("notes")) v.setNotes((String) m.get("notes"));
                        v.evaluateCategories();
                        dataStore.addVital(v);
                        sendJsonResponse(exchange, 201, v);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteVital(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Vital not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/nutrition" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getNutritionLogsForUser(targetUser));
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        NutritionLog n = new NutritionLog();
                        n.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        n.setTimestamp(m.containsKey("timestamp") ? (String) m.get("timestamp") : LocalDateTime.now().toString());
                        if (m.containsKey("mealType")) n.setMealType((String) m.get("mealType"));
                        if (m.containsKey("foodName")) n.setFoodName((String) m.get("foodName"));
                        if (m.containsKey("calories")) n.setCalories(((Number) m.get("calories")).intValue());
                        if (m.containsKey("proteinGrams")) n.setProteinGrams(((Number) m.get("proteinGrams")).doubleValue());
                        if (m.containsKey("carbsGrams")) n.setCarbsGrams(((Number) m.get("carbsGrams")).doubleValue());
                        if (m.containsKey("fatGrams")) n.setFatGrams(((Number) m.get("fatGrams")).doubleValue());
                        if (m.containsKey("fiberGrams")) n.setFiberGrams(((Number) m.get("fiberGrams")).doubleValue());
                        if (m.containsKey("waterMl")) n.setWaterMl(((Number) m.get("waterMl")).intValue());
                        if (m.containsKey("notes")) n.setNotes((String) m.get("notes"));
                        dataStore.addNutritionLog(n);
                        sendJsonResponse(exchange, 201, n);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteNutritionLog(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Nutrition log not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/sleep" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        sendJsonResponse(exchange, 200, dataStore.getSleepSessionsForUser(targetUser));
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        SleepSession s = new SleepSession();
                        s.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        if (m.containsKey("sleepStart")) s.setSleepStart((String) m.get("sleepStart"));
                        if (m.containsKey("sleepEnd")) s.setSleepEnd((String) m.get("sleepEnd"));
                        if (m.containsKey("deepMinutes")) s.setDeepMinutes(((Number) m.get("deepMinutes")).intValue());
                        if (m.containsKey("lightMinutes")) s.setLightMinutes(((Number) m.get("lightMinutes")).intValue());
                        if (m.containsKey("remMinutes")) s.setRemMinutes(((Number) m.get("remMinutes")).intValue());
                        if (m.containsKey("awakeMinutes")) s.setAwakeMinutes(((Number) m.get("awakeMinutes")).intValue());
                        if (m.containsKey("notes")) s.setNotes((String) m.get("notes"));
                        s.calculateSleepScore();
                        dataStore.addSleepSession(s);
                        sendJsonResponse(exchange, 201, s);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteSleepSession(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Sleep session not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/cycle" -> {
                    if ("GET".equalsIgnoreCase(method)) {
                        Map<String, Object> response = new LinkedHashMap<>();
                        UserProfile up = dataStore.getUser(targetUser);
                        if (up == null) up = dataStore.getActiveUser();
                        List<CycleLog> logs = dataStore.getCycleLogsForUser(up.getId());
                        response.put("logs", logs);
                        LocalDate anchor = LocalDate.now().minusDays(10);
                        if (!logs.isEmpty() && logs.get(0).getCycleStartDate() != null) {
                            try {
                                anchor = LocalDate.parse(logs.get(0).getCycleStartDate());
                            } catch (Exception ignored) {}
                        }
                        if ("FEMALE".equalsIgnoreCase(up.getGender())) {
                            response.put("status", CyclePredictor.evaluateCycle(anchor, up));
                        } else {
                            response.put("status", null);
                        }
                        sendJsonResponse(exchange, 200, response);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        String body = readBody(exchange);
                        Map<String, Object> m = JsonUtil.parseObject(body);
                        CycleLog c = new CycleLog();
                        c.setUserId(m.containsKey("userId") ? (String) m.get("userId") : targetUser);
                        c.setLogDate(m.containsKey("logDate") ? (String) m.get("logDate") : LocalDate.now().toString());
                        c.setCycleStartDate(m.containsKey("cycleStartDate") ? (String) m.get("cycleStartDate") : c.getLogDate());
                        if (m.containsKey("cycleDay")) c.setCycleDay(((Number) m.get("cycleDay")).intValue());
                        if (m.containsKey("phase")) c.setPhase((String) m.get("phase"));
                        if (m.containsKey("flow")) c.setFlow((String) m.get("flow"));
                        if (m.containsKey("mood")) c.setMood((String) m.get("mood"));
                        if (m.containsKey("cervicalMucus")) c.setCervicalMucus((String) m.get("cervicalMucus"));
                        if (m.containsKey("basalBodyTempC")) c.setBasalBodyTempC(((Number) m.get("basalBodyTempC")).doubleValue());
                        if (m.containsKey("notes")) c.setNotes((String) m.get("notes"));
                        if (m.get("symptoms") instanceof List<?> symList) {
                            List<String> list = new ArrayList<>();
                            for (Object o : symList) if (o != null) list.add(o.toString());
                            c.setSymptoms(list);
                        }
                        UserProfile up = dataStore.getUser(c.getUserId());
                        if (up == null) up = dataStore.getActiveUser();
                        LocalDate cycleStart = LocalDate.parse(c.getCycleStartDate());
                        var evaluated = CyclePredictor.evaluateCycle(cycleStart, up);
                        c.setPredictedNextPeriod(evaluated.nextPeriodDate().toString());
                        c.setPredictedOvulationDate(evaluated.nextOvulationDate().toString());
                        c.setFertileWindow(evaluated.isFertileWindow());
                        c.setOvulationDay(evaluated.isOvulationToday());

                        dataStore.addCycleLog(c);
                        sendJsonResponse(exchange, 201, c);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        String id = queryParams.get("id");
                        if (id != null && dataStore.deleteCycleLog(id)) {
                            sendJsonResponse(exchange, 200, Map.of("success", true));
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Cycle log not found"));
                        }
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                case "/api/reset-demo" -> {
                    if ("POST".equalsIgnoreCase(method)) {
                        dataStore.seedDemoData();
                        dataStore.saveToFile();
                        sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Multi-user demo data reset successfully"));
                    } else {
                        sendResponse(exchange, 405, "Method Not Allowed");
                    }
                }
                default -> sendJsonResponse(exchange, 404, Map.of("error", "Endpoint not found: " + path));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            } else if (kv.length == 1) {
                params.put(kv[0], "");
            }
        }
        return params;
    }
}
