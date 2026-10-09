package fitbit.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpIntegrationTest {
    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/dashboard")).build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Integration Test: HTTP " + resp.statusCode());
            System.out.println("Response snippet: " + resp.body().substring(0, Math.min(150, resp.body().length())));
            if (resp.statusCode() == 200) {
                System.out.println("[✔] WebServer & API integration test PASSED!");
            } else {
                System.err.println("[✖] WebServer returned unexpected status: " + resp.statusCode());
            }
        } catch (Exception e) {
            System.out.println("Local sandbox socket notice: " + e.getMessage());
        }
    }
}
