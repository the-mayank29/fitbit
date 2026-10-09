-- ============================================================================
-- FITBIT MULTI-USER DATABASE SCHEMA DESIGN (SQLite / Relational SQL)
-- Third Normal Form (3NF) relational database for multiple users
-- ============================================================================

PRAGMA foreign_keys = ON;

-- 1. Users Table (Multi-User Accounts & Profiles)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) UNIQUE,
    avatar_color VARCHAR(30) DEFAULT '#1a73e8',
    age INTEGER CHECK (age > 0 AND age <= 120),
    gender VARCHAR(20) DEFAULT 'OTHER',
    height_cm REAL CHECK (height_cm > 0),
    target_weight_kg REAL CHECK (target_weight_kg > 0),
    daily_step_goal INTEGER DEFAULT 10000,
    daily_calorie_goal INTEGER DEFAULT 600,
    daily_water_goal_ml INTEGER DEFAULT 2500,
    sleep_target_hours REAL DEFAULT 8.0,
    cycle_length_days INTEGER DEFAULT 28,
    period_length_days INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Activities & Workouts Table (Linked to User via Foreign Key)
CREATE TABLE IF NOT EXISTS activities (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp DATETIME NOT NULL,
    duration_minutes REAL NOT NULL CHECK (duration_minutes > 0),
    distance_km REAL DEFAULT 0.0,
    calories_burned INTEGER NOT NULL CHECK (calories_burned >= 0),
    steps INTEGER DEFAULT 0,
    avg_heart_rate INTEGER,
    max_heart_rate INTEGER,
    intensity VARCHAR(30) DEFAULT 'MODERATE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Body Measurements Table
CREATE TABLE IF NOT EXISTS body_measurements (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    timestamp DATETIME NOT NULL,
    weight_kg REAL NOT NULL CHECK (weight_kg > 0),
    height_cm REAL NOT NULL CHECK (height_cm > 0),
    bmi REAL NOT NULL,
    bmi_category VARCHAR(30),
    body_fat_percent REAL,
    muscle_mass_kg REAL,
    chest_cm REAL,
    waist_cm REAL,
    hips_cm REAL,
    biceps_cm REAL,
    thighs_cm REAL,
    waist_to_hip_ratio REAL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Vitals & Biometrics Table
CREATE TABLE IF NOT EXISTS vitals (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    timestamp DATETIME NOT NULL,
    heart_rate_bpm INTEGER NOT NULL CHECK (heart_rate_bpm > 0),
    resting_heart_rate_bpm INTEGER,
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    spo2_percent REAL CHECK (spo2_percent >= 0 AND spo2_percent <= 100),
    blood_glucose_mg_dl REAL,
    body_temp_c REAL,
    bp_category VARCHAR(40),
    hr_zone VARCHAR(40),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Nutrition & Hydration Logs Table
CREATE TABLE IF NOT EXISTS nutrition_logs (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    timestamp DATETIME NOT NULL,
    meal_type VARCHAR(40) NOT NULL,
    food_name VARCHAR(150) NOT NULL,
    calories INTEGER NOT NULL CHECK (calories >= 0),
    protein_grams REAL DEFAULT 0.0,
    carbs_grams REAL DEFAULT 0.0,
    fat_grams REAL DEFAULT 0.0,
    fiber_grams REAL DEFAULT 0.0,
    water_ml INTEGER DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Sleep Sessions Table
CREATE TABLE IF NOT EXISTS sleep_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    sleep_start DATETIME NOT NULL,
    sleep_end DATETIME NOT NULL,
    total_minutes INTEGER NOT NULL CHECK (total_minutes > 0),
    deep_minutes INTEGER NOT NULL,
    light_minutes INTEGER NOT NULL,
    rem_minutes INTEGER NOT NULL,
    awake_minutes INTEGER NOT NULL,
    efficiency_percent REAL,
    sleep_score INTEGER NOT NULL CHECK (sleep_score >= 0 AND sleep_score <= 100),
    quality VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. Menstrual Cycle Logs Table
CREATE TABLE IF NOT EXISTS cycle_logs (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    log_date DATE NOT NULL,
    cycle_start_date DATE NOT NULL,
    cycle_day INTEGER NOT NULL CHECK (cycle_day >= 1),
    phase VARCHAR(40) NOT NULL,
    flow VARCHAR(30),
    symptoms TEXT,
    mood VARCHAR(50),
    cervical_mucus VARCHAR(50),
    basal_body_temp_c REAL,
    is_fertile_window BOOLEAN DEFAULT 0,
    is_ovulation_day BOOLEAN DEFAULT 0,
    predicted_next_period DATE,
    predicted_ovulation_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for high-speed multi-user queries
CREATE INDEX IF NOT EXISTS idx_activities_user ON activities(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_measurements_user ON body_measurements(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_vitals_user ON vitals(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_nutrition_user ON nutrition_logs(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_sleep_user ON sleep_sessions(user_id, sleep_start);
CREATE INDEX IF NOT EXISTS idx_cycle_user ON cycle_logs(user_id, log_date);

-- ============================================================================
-- SEED DATA FOR MULTIPLE USERS
-- ============================================================================

-- Insert Users
INSERT OR REPLACE INTO users (id, name, email, avatar_color, age, gender, height_cm, target_weight_kg, daily_step_goal, daily_calorie_goal, daily_water_goal_ml, cycle_length_days, period_length_days)
VALUES
('user_1', 'Alex Morgan', 'alex.morgan@fitbit.app', '#1a73e8', 28, 'FEMALE', 168.0, 60.0, 10000, 600, 2500, 28, 5),
('user_2', 'David Chen', 'david.chen@fitbit.app', '#00875a', 34, 'MALE', 182.0, 80.0, 8000, 750, 3200, 0, 0),
('user_3', 'Elena Rostova', 'elena.r@fitbit.app', '#9334e6', 25, 'FEMALE', 164.0, 54.0, 12000, 500, 2400, 29, 4);

-- Insert Sample Activities for Multiple Users
INSERT OR REPLACE INTO activities (id, user_id, name, type, timestamp, duration_minutes, distance_km, calories_burned, steps, avg_heart_rate, intensity, notes)
VALUES
('act_1', 'user_1', 'Morning 5K Run', 'RUNNING', datetime('now', '-2 days'), 32.0, 5.2, 380, 5800, 142, 'HIGH', 'Alex morning pace'),
('act_2', 'user_1', 'Outdoor Road Cycling', 'CYCLING', datetime('now'), 45.0, 16.5, 420, 6800, 138, 'HIGH', 'Hilly road'),
('act_3', 'user_2', 'Heavy Push Day (Chest & Triceps)', 'STRENGTH', datetime('now', '-1 days'), 60.0, 0.0, 440, 2500, 136, 'HIGH', 'David lifting heavy'),
('act_4', 'user_2', 'Incline Treadmill Walk', 'WALKING', datetime('now'), 35.0, 3.2, 260, 4100, 115, 'MODERATE', 'Fat burn cardio'),
('act_5', 'user_3', 'Kundalini Yoga & Breathwork', 'YOGA', datetime('now'), 45.0, 0.0, 170, 1500, 98, 'MODERATE', 'Elena morning session');
