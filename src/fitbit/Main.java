package fitbit;

import fitbit.repository.DataStore;
import fitbit.server.WebServer;

import java.io.IOException;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("===============================================================");
        System.out.println("       FITBIT - MINIMAL HEALTH & FITNESS PLATFORM             ");
        System.out.println("                 (Google Fit Minimal UI Style)                 ");
        System.out.println("                      Powered by Java 21                       ");
        System.out.println("===============================================================");
        System.out.println(" Initializing Health DataStore & Persistence Engine...");

        DataStore dataStore = new DataStore();

        try {
            WebServer webServer = new WebServer(port, dataStore);
            webServer.start();

            System.out.println(" [✔] Server started successfully on port " + port);
            System.out.println(" [✔] Virtual Threads Executor enabled");
            System.out.println(" [✔] REST API mounted at:       http://localhost:" + port + "/api");
            System.out.println(" [✔] Google Fit UI live at:     http://localhost:" + port + "/");
            System.out.println("---------------------------------------------------------------");
            System.out.println(" MODULES READY:");
            System.out.println("  1. Activities & Workouts (Steps, Move Min, Heart Points)");
            System.out.println("  2. Body Measurements (Weight, Height, BMI, Body Fat %)");
            System.out.println("  3. Vitals & Biometrics (Heart Rate, Blood Pressure, SpO2)");
            System.out.println("  4. Nutrition & Hydration (Meals, Macros, Water tracker)");
            System.out.println("  5. Sleep Architecture (Duration, Sleep Score, Stages)");
            System.out.println("  6. Cycle Tracking (Phases, Ovulation, Symptoms, Mood)");
            System.out.println("  7. Minimal Material Design 3 Concentric Activity Rings");
            System.out.println("---------------------------------------------------------------");
            System.out.println(" Open your browser at: http://localhost:" + port);
            System.out.println(" Press Ctrl+C to terminate the server.");
            System.out.println("===============================================================");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nSaving data and shutting down server...");
                dataStore.saveToFile();
                webServer.stop();
                System.out.println("Fitbit server stopped safely.");
            }));

        } catch (IOException e) {
            System.err.println("Fatal: Could not start web server on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
