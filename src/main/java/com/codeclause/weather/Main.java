package com.codeclause.weather;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.Properties;
import java.util.Scanner;
import org.json.JSONObject;

public class Main {
    private static final String API_URL =
        "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s";

    public static void main(String[] args) {
        String apiKey = loadApiKey();
        if (apiKey == null) {
            System.err.println("ERROR: API key not found. Configure config.properties.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter city name: ");
        String city = scanner.nextLine().trim();

        try {
            String json = fetchWeatherData(city, apiKey);
            displayWeather(json);
        } catch (IOException e) {
            if (e.getMessage().contains("404")) {
                System.err.println("City not found. Check spelling and try again.");
            } else {
                System.err.println("Network/API error: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            System.err.println("Request interrupted. Try again.");
            Thread.currentThread().interrupt();
        }
    }

    private static String loadApiKey() {
        try (InputStream in = Main.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String k = p.getProperty("OPENWEATHER_KEY");
                if (k != null && !k.isBlank()) return k;
            }
        } catch (IOException ignored) { }
        return System.getenv("OPENWEATHER_KEY");
    }

    private static String fetchWeatherData(String city, String key)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String uri = String.format(API_URL, city, key);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(uri)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("API returned status " + resp.statusCode());
        }
        return resp.body();
    }private static void displayWeather(String json) {
        JSONObject o = new JSONObject(json);
        String name = o.getString("name");
        JSONObject m = o.getJSONObject("main");
        double t = m.getDouble("temp");
        int h = m.getInt("humidity");
        String desc = o.getJSONArray("weather")
                       .getJSONObject(0)
                       .getString("description");
        System.out.printf("%nWeather in %s:%n", name);
        System.out.printf("Temperature: %.1f °C%nHumidity: %d%%%nConditions: %s%n",t, h, desc);
    }
}
