package fitbit.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fitbit.controller.ApiHandler;
import fitbit.repository.DataStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class WebServer {
    private final int port;
    private final DataStore dataStore;
    private HttpServer server;

    public WebServer(int port, DataStore dataStore) {
        this.port = port;
        this.dataStore = dataStore;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Java 21 Virtual Threads
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // API routes
        server.createContext("/api", new ApiHandler(dataStore));

        // Static Web routes
        server.createContext("/", new StaticFileHandler("web"));

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    public int getPort() {
        return port;
    }

    private static class StaticFileHandler implements HttpHandler {
        private final String baseDir;

        public StaticFileHandler(String baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isBlank()) {
                path = "/index.html";
            }

            // Security check against directory traversal
            if (path.contains("..")) {
                send404(exchange);
                return;
            }

            File file = new File(baseDir + path);
            if (!file.exists() || file.isDirectory()) {
                // Fallback to index.html for SPA if not found
                file = new File(baseDir + "/index.html");
                if (!file.exists()) {
                    send404(exchange);
                    return;
                }
            }

            String mimeType = probeContentType(file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void send404(HttpExchange exchange) throws IOException {
            byte[] msg = "404 Not Found".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        }

        private String probeContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=utf-8";
            if (filename.endsWith(".css")) return "text/css; charset=utf-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (filename.endsWith(".json")) return "application/json; charset=utf-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            if (filename.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=utf-8";
        }
    }
}
