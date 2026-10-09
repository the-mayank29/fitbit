# ⚡ FITBIT 3D - Next-Gen Health & Biometric Suite

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Database](https://img.shields.io/badge/Database-SQLite%203%20%2F%20JDBC-blue.svg)](https://www.sqlite.org/)
[![UI](https://img.shields.io/badge/UI-WebGL%203D%20Interactive-cyan.svg)](web/index.html)
[![Evaluation](https://img.shields.io/badge/Review%201-100%25%20Rubric%20Compliant-success.svg)](presentation/Review1_Presentation.html)

A modular, full-stack fitness and biometric tracker engineered on **Java 21**, featuring an interactive **3D WebGL Biometric Visualization Engine**, normalized **SQLite 3 Relational Database**, and full compliance with the **Java GUI Project Marking Rubric**.

---

## 🎯 Review 1 Marking Rubric Mapping

| Rubric Component | Marks | Project Implementation | Key Files |
|---|---|---|---|
| **OOP: Inheritance** | **10 Marks** (Combined) | Multi-level inheritance: `BaseEntity` (abstract) &rarr; `HealthMetric` (abstract) &rarr; `Activity`, `Vital`, `BodyMeasurement`, `NutritionLog`, `SleepSession`, `CycleLog` | [`BaseEntity.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/model/BaseEntity.java)<br>[`HealthMetric.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/model/HealthMetric.java) |
| **OOP: Polymorphism** | | Dynamic runtime method dispatch using `Trackable` interface, overridden `validate()`, `getSummary()`, `getPrimaryMetricValue()` | [`Trackable.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/model/Trackable.java)<br>[`Activity.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/model/Activity.java) |
| **OOP: Interfaces** | | Interface contracts for tracking metrics (`Trackable`) and generic persistence (`Repository<T, ID>`) | [`Trackable.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/model/Trackable.java)<br>[`Repository.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/repository/Repository.java) |
| **OOP: Exception Handling** | | Custom domain exception hierarchy: `FitnessException` (base) &rarr; `ValidationException`, `EntityNotFoundException`, `DatabaseException` with error codes | [`FitnessException.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/exception/FitnessException.java)<br>[`ValidationException.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/exception/ValidationException.java) |
| **Collections & Generics** | **6 Marks** | Type-safe Generic Repository `Repository<T extends BaseEntity, ID>`, `GenericRepository` with `ConcurrentHashMap`, `ArrayList`, Streams API, and Predicate filtering | [`Repository.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/repository/Repository.java)<br>[`GenericRepository.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/repository/GenericRepository.java) |
| **Database Design** | Core Req | 7 Normalized (3NF) relational tables with Foreign Keys, Check Constraints, and Performance Indexes in SQLite | [`schema.sql`](file:///Users/mayank/Downloads/fitbit/data/schema.sql)<br>[`fitbit.db`](file:///Users/mayank/Downloads/fitbit/data/fitbit.db) |
| **Database Connectivity** | Core Req | JDBC connection management using Singleton Pattern (`DbConnectionFactory`), `PreparedStatement`, and resource management | [`DbConnectionFactory.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/db/DbConnectionFactory.java)<br>[`DatabaseManager.java`](file:///Users/mayank/Downloads/fitbit/src/fitbit/db/DatabaseManager.java) |
| **UI/UX Aesthetics & Responsiveness** | Core Req | Responsive Cyber-Biometric Dark UI + 5 Interactive WebGL 3D Visualization Modes (Body Avatar, Heart, Rings, Orb, Disc) | [`index.html`](file:///Users/mayank/Downloads/fitbit/web/index.html)<br>[`engine3d.js`](file:///Users/mayank/Downloads/fitbit/web/js/engine3d.js) |

---

## 📊 Database Design (ER Diagram)

The SQLite database (`data/fitbit.db`) is structured according to Third Normal Form (3NF):

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

## 🏛️ OOP Class Hierarchy

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
    class Trackable {
        <<interface>>
        +getTimestamp()
        +getCategory()
        +getPrimaryMetricValue()
        +getSummary()
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
    }
    class BodyMeasurement {
        -double weightKg
        -double bmi
    }
    class Vital {
        -int heartRateBpm
        -int systolicBp
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

    BaseEntity <|-- HealthMetric
    Trackable <|.. HealthMetric
    HealthMetric <|-- Activity
    HealthMetric <|-- BodyMeasurement
    HealthMetric <|-- Vital
    HealthMetric <|-- NutritionLog
    HealthMetric <|-- SleepSession
    HealthMetric <|-- CycleLog
```

---

## 🚀 How to Run the Project

### Option 1: One-Click Launch Script
```bash
cd /Users/mayank/Downloads/fitbit
./run.sh
```
* Compiles all Java files.
* Starts the web server on `http://localhost:8080`.
* Automatically launches your web browser.

### Option 2: Run Review 1 Verification Tests
```bash
java -ea -cp bin fitbit.test.TestRunner
```

---

## 📽️ Review 1 Presentation File Deliverables

You have two presentation formats prepared in the `presentation/` directory:

1. **Interactive HTML Slide Deck (`presentation/Review1_Presentation.html`):**
   - Open in any browser:
     ```bash
     open presentation/Review1_Presentation.html
     ```
   - Features keyboard arrow navigation (<kbd>&larr;</kbd> / <kbd>&rarr;</kbd>).
   - Click **"🖨️ Export PDF / Print"** (or press <kbd>Cmd</kbd> + <kbd>P</kbd>) to save as a submission-ready PDF file!
2. **Markdown Presentation Document (`presentation/Review1_Presentation.md`):**
   - Contains slide content, speaker script, bullet points, and diagram definitions.

---

## 🐙 How to Push to GitHub (For Review 1 Submission)

Run these commands in your terminal:

```bash
cd /Users/mayank/Downloads/fitbit

# 1. Add all files to git
git add .

# 2. Create the Review 1 commit
git commit -m "Complete Review 1: Project structure, Database design, JDBC connectivity, OOP architecture, and 3D UI"

# 3. Create a new public repository on GitHub (e.g. named fitbit-3d)
# Then link your repository and push:
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/fitbit-3d.git
git push -u origin main
```
