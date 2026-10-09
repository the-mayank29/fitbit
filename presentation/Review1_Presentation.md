# ⚡ Review 1 Presentation: FITBIT Health & Fitness Application

> **Project Title:** FITBIT - Minimal Health & Biometric Tracking Platform (Google Fit Style)  
> **Course / Review:** Java GUI Based Projects — Review 1 Evaluation  
> **Tech Stack:** Java 21 LTS, SQLite 3 / JDBC, Google Fit Material Design 3, Virtual Threads  

---

## 📑 Slide Deck Outline & Speaker Script

### Slide 1: Title & Introduction
* **Headline:** FITBIT - Minimal Health & Biometric Tracking Platform
* **Presenter:** Student Project Team
* **Review Focus:** Project Structure, Database Design, Database Connectivity, OOP Architecture, Collections & Generics, and Minimal UI/UX.
* **Speaker Script:**  
  *"Good morning reviewers. Today, we present Review 1 of our project, Fitbit. Our goal is to provide a clean, minimal health tracking experience inspired by Google Fit, built on a robust Java 21 backend with SQLite relational persistence."*

---

### Slide 2: Project Scope & Core Modules
* **Domain Modules Covered:**
  1. **🏃 Activities:** Workouts (Running, Cycling, Strength, HIIT, Yoga) tracking duration, distance, calories, and Heart Points.
  2. **📐 Body Measurements:** Weight, Height, Body Fat %, Muscle Mass, tape circumferences with BMI & Waist-to-Hip ratio calculation.
  3. **💓 Vitals:** Heart rate BPM, resting HR, blood pressure with AHA clinical classification, SpO2, blood glucose, and temperature.
  4. **🥗 Nutrition & Hydration:** Meals, macronutrients (protein, carbs, fat), and interactive water intake counter.
  5. **🛌 Sleep:** Sleep stages (Deep, Light, REM, Awake), efficiency %, 0–100 sleep recovery score, and Canvas hypnogram.
  6. **🌸 Menstrual & Cycle Tracking:** 4-phase cycle tracking, ovulation countdown, fertile window highlight, and symptoms.

---

### Slide 3: Modular Project Architecture
* **Directory Layout & Clean Layering:**
```
fitbit/
├── src/fitbit/
│   ├── model/         # Domain entities (BaseEntity, HealthMetric, Activity, etc.)
│   ├── repository/    # Generic Repository<T, ID> & DataStore
│   ├── db/            # DatabaseConfig, DbConnectionFactory, DatabaseManager
│   ├── exception/     # FitnessException, ValidationException, DatabaseException
│   ├── analytics/     # HealthCalculator, CyclePredictor
│   ├── controller/    # ApiHandler (REST endpoints)
│   ├── server/        # WebServer (Virtual Threads) & JsonUtil
│   └── Main.java      # Application Bootstrap
├── data/
│   ├── schema.sql     # Relational 3NF SQL DDL
│   └── fitbit.db      # SQLite Database
├── web/               # Google Fit Minimal UI (HTML5/CSS3/JS)
└── presentation/      # Review 1 Slide Deck & Docs
```

---

### Slide 4: OOP Implementation — Inheritance Hierarchy (10 Marks Rubric)
```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        #String id
        #String createdAt
        #String updatedAt
        +validate()*
        +getEntityType()*
    }
    class UserProfile {
        -String name
        -int age
        -double heightCm
        -double targetWeightKg
    }
    class HealthMetric {
        <<abstract>>
        #String timestamp
        #String notes
        +validate()
    }
    class Activity {
        -String type
        -double durationMinutes
        -int caloriesBurned
        +getCategory()
        +getPrimaryMetricValue()
    }
    class BodyMeasurement {
        -double weightKg
        -double bmi
        -double waistToHipRatio
    }
    class Vital {
        -int heartRateBpm
        -int systolicBp
        -int diastolicBp
    }
    class NutritionLog {
        -String mealType
        -int calories
    }
    class SleepSession {
        -int sleepScore
        -int deepMinutes
    }
    class CycleLog {
        -int cycleDay
        -String phase
    }

    BaseEntity <|-- UserProfile
    BaseEntity <|-- HealthMetric
    HealthMetric <|-- Activity
    HealthMetric <|-- BodyMeasurement
    HealthMetric <|-- Vital
    HealthMetric <|-- NutritionLog
    HealthMetric <|-- SleepSession
    HealthMetric <|-- CycleLog
```
* **Key OOP Principles:**
  - Multi-level inheritance (`BaseEntity` &rarr; `HealthMetric` &rarr; `Activity`).
  - Code reusability for common fields (timestamps, IDs, notes).
  - Enforced validation template method across all subclasses.

---

### Slide 5: OOP Implementation — Polymorphism & Interfaces
* **Interface `Trackable`:**
  ```java
  public interface Trackable {
      String getTimestamp();
      String getCategory();
      double getPrimaryMetricValue();
      String getSummary();
  }
  ```
* **Dynamic Method Dispatch:**
  Subclasses override `getCategory()`, `getPrimaryMetricValue()`, and `getSummary()`. When iterating through a `List<Trackable>`, the JVM dispatches the appropriate specialized implementation at runtime.

---

### Slide 6: OOP Implementation — Exception Handling Hierarchy
* **Custom Exception Architecture:**
  - `FitnessException` (Root domain exception with error codes)
  - `ValidationException` (Subclass thrown when metric inputs violate clinical or realistic ranges)
  - `EntityNotFoundException` (Subclass thrown on missing database records)
  - `DatabaseException` (Subclass wrapping SQL and connection errors)
* **Pre-condition Validation:** Entities self-validate before state persistence using `validate()`.

---

### Slide 7: Collections & Generics (6 Marks Rubric)
* **Generic Repository Interface:**
  ```java
  public interface Repository<T extends BaseEntity, ID> {
      T save(T entity);
      Optional<T> findById(ID id);
      List<T> findAll();
      boolean deleteById(ID id);
      List<T> filter(Predicate<T> predicate);
      List<T> findSorted(Comparator<T> comparator);
  }
  ```
* **Collections Used:**
  - `ConcurrentHashMap<ID, T>` for thread-safe memory storage.
  - `ArrayList<T>` for dynamic list queries.
  - Java Stream API (`stream().filter(...).sorted(...).collect(...)`) for generic functional filtering.

---

### Slide 8: Database Design — Relational Schema (3NF)
```mermaid
erDiagram
    users ||--o{ activities : logs
    users ||--o{ body_measurements : records
    users ||--o{ vitals : tracks
    users ||--o{ nutrition_logs : consumes
    users ||--o{ sleep_sessions : sleeps
    users ||--o{ cycle_logs : cycles

    users {
        string id PK
        string name
        int age
        real height_cm
        real target_weight_kg
        int daily_step_goal
    }
    activities {
        string id PK
        string user_id FK
        string type
        real duration_minutes
        int calories_burned
    }
    body_measurements {
        string id PK
        string user_id FK
        real weight_kg
        real bmi
        real waist_to_hip_ratio
    }
    vitals {
        string id PK
        string user_id FK
        int heart_rate_bpm
        int systolic_bp
        int diastolic_bp
    }
    nutrition_logs {
        string id PK
        string user_id FK
        string meal_type
        int calories
    }
    sleep_sessions {
        string id PK
        string user_id FK
        int sleep_score
        int deep_minutes
    }
    cycle_logs {
        string id PK
        string user_id FK
        int cycle_day
        string phase
    }
```

---

### Slide 9: Database Connectivity (JDBC)
* **`DbConnectionFactory`:** Thread-safe Singleton factory establishing connections to SQLite (`jdbc:sqlite:data/fitbit.db`).
* **`DatabaseManager`:** Manages table integrity, queries schema metadata, and uses `PreparedStatement` to prevent SQL injection.
* **Dual-Persistence Mode:** Supports both SQL database operations and zero-dependency JSON serialization.

---

### Slide 10: UI/UX Design — Google Fit Minimal Material Design
* **Design Principles:**
  - **Concentric Activity Rings:** Outer Blue Ring (`#1a73e8`) for Move Minutes & Steps; Inner Green Ring (`#00875a`) for Heart Points.
  - **Material Design 3 Palette:** Clean cards, generous padding, subtle elevation, and Google's signature color coding.
  - **Speed-Dial Floating Action Button (FAB):** Quick one-tap logging for activities, weight, vitals, meals, sleep, and cycle logs.
  - **Clean Journal Feed:** Chronological cards for workouts and health logs with zero visual clutter.
* **Performance & Accessibility:** Lightweight, responsive across mobile, tablet, and desktop without WebGL overhead.

---

### Slide 11: Review 1 Rubric Mapping
| Marking Rubric Criteria | Allocated Marks | Implementation in Project |
|---|---|---|
| **OOP: Inheritance** | Part of 10 | `BaseEntity` &rarr; `HealthMetric` &rarr; Concrete Logs |
| **OOP: Polymorphism** | Part of 10 | `Trackable` interface dispatch, overridden `validate()` & `getSummary()` |
| **OOP: Interfaces** | Part of 10 | `Trackable`, `Repository<T, ID>` |
| **OOP: Exception Handling** | Part of 10 | `FitnessException`, `ValidationException`, `DatabaseException` |
| **Collections & Generics** | **6 Marks** | `Repository<T, ID>`, `GenericRepository`, `ConcurrentHashMap`, Streams |
| **Database Design** | Core Req | 7 Normalized 3NF tables in SQLite with Foreign Keys & Indexes |
| **Database Connectivity** | Core Req | JDBC `DbConnectionFactory`, Singleton Pattern, `PreparedStatement` |
| **UI/UX Aesthetics & Responsiveness** | Core Req | Google Fit Minimal UI with Concentric Activity Rings & FAB |
