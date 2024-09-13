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

    public String fetchOddsForSportAndMarket(String sportKey, String marketType) {
        String url = baseUrl + sportKey + "/odds/?apiKey="
                + apiKey + "&regions=us&markets=" + marketType + "&oddsFormat=american";

        try {
            // Create URL object
            URL obj = new URL(url);

            // Open connection
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Set request method
            con.setRequestMethod("GET");

            // Get the response code
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            System.out.println("x-requests-remaining: " + con.getHeaderField("x-requests-remaining"));
            System.out.println("x-requests-used: " + con.getHeaderField("x-requests-used"));
            System.out.println("x-requests-last: " + con.getHeaderField("x-requests-last"));

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the input stream from the connection
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Close the reader
                in.close();

                // Return the response as a String
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

    public void saveResponseToFile(String jsonResponse, String fileName) {
        try (FileWriter file = new FileWriter(fileName)) {
            file.write(jsonResponse);
            System.out.println("Successfully saved odds to " + fileName);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
