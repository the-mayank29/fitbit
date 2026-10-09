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
        System.out.println("       FITBIT 3D - NEXT-GEN FITNESS & HEALTH TRACKER           ");
        System.out.println("                   Powered by Java 21                         ");
        System.out.println("===============================================================");
        System.out.println(" Initializing Health DataStore & Persistence Engine...");

        DataStore dataStore = new DataStore();

        try {
            WebServer webServer = new WebServer(port, dataStore);
            webServer.start();

            System.out.println(" [✔] Server started successfully on port " + port);
            System.out.println(" [✔] Virtual Threads Executor enabled");
            System.out.println(" [✔] REST API mounted at:       http://localhost:" + port + "/api");
            System.out.println(" [✔] 3D Interactive UI live at:  http://localhost:" + port + "/");
            System.out.println("---------------------------------------------------------------");
            System.out.println(" FEATURES LOADED:");
            System.out.println("  1. Activities (Workouts, Steps, Active Calories, Intensity)");
            System.out.println("  2. Body Measurements (Weight, BMI, Body Fat %, 3D Avatar)");
            System.out.println("  3. Vitals (3D Beating Heart, BPM, BP, SpO2, Glucose, Temp)");
            System.out.println("  4. Nutrition (Meal Logs, Macro breakdown, Hydration cup)");
            System.out.println("  5. Sleep (Sleep Score, Deep/Light/REM/Awake, 3D Sleep Orb)");
            System.out.println("  6. Cycle Tracking (Phases, Ovulation, Fertile Window, Mood)");
            System.out.println("  7. 3D WebGL Visualization Engine (Avatar, Heart, Rings, Orb)");
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
