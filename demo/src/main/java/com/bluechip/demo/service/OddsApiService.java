package com.bluechip.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class OddsApiService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String baseUrl;

    @Value("${data.file-path}")
    private String dataFilePath;

    public String fetchOddsForSportAndMarket(String sportKey, String marketType) {

        // Build URL (note: "/odds?" not "/odds/?")
        String url = baseUrl
                + sportKey
                + "/odds"
                + "?regions=" + enc("us,us2")
                + "&markets=" + enc(marketType)
                + "&oddsFormat=" + enc("american")
                + "&apiKey=" + enc(apiKey);

        // Log final URL (redact the key for safety)
        System.out.println("Final API URL: " + url.replace(apiKey, "******"));

        HttpURLConnection con = null;
        try {
            URL obj = new URL(url);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(15_000);
            con.setReadTimeout(30_000);

            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            String remaining = con.getHeaderField("x-requests-remaining");
            String used = con.getHeaderField("x-requests-used");
            String last = con.getHeaderField("x-requests-last");

            System.out.println("x-requests-remaining: " + (remaining != null ? remaining : "<none>"));
            System.out.println("x-requests-used: " + (used != null ? used : "<none>"));
            System.out.println("x-requests-last: " + (last != null ? last : "<none>"));
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                // Read error body for diagnostics
                String errorBody = readBody(con.getErrorStream());
                System.out.println("API error body: " + (errorBody == null ? "<empty>" : errorBody));
                throw new IOException("Failed to fetch data from API (code " + responseCode + ")");
            }
        } catch (IOException e) {
            System.out.println("Error during communication: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String readBody(InputStream is) {
        if (is == null) return null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException ex) {
            return "<failed to read error stream: " + ex.getMessage() + ">";
        }
    }
}