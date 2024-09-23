package com.bluechip.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class OddsApiService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String baseUrl;

    @Value("${data.file-path}")
    private String dataFilePath;

    public String fetchOddsForSportAndMarket(String sportKey, String marketType) {
        String url = baseUrl + sportKey + "/odds/?apiKey="
                + apiKey + "&regions=us,us2&markets=" + marketType + "&oddsFormat=american";

        try {
            // Create URL object and connect
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            // Get the response code
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                System.out.println("GET request failed. Response Code: " + responseCode);
                throw new IOException("Failed to fetch data from API");
            }
        } catch (IOException e) {
            System.out.println("Error during communication: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void saveResponseToFile(String jsonResponse, File file) {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonResponse);
            System.out.println("Successfully saved odds to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}