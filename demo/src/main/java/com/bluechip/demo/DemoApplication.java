package com.bluechip.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

/*
String apiKey = "07a2f7ce63288ec344326c997f7fbf3c";
String url = "https://api.the-odds-api.com/v4/sports/americanfootball_nfl/odds/?apiKey="
		+ apiKey + "&regions=us&markets=h2h,spreads&oddsFormat=american";

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
    // If the request was successful (status code 200)
    if (responseCode == HttpURLConnection.HTTP_OK) {
        // Read the input stream from the connection
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        // Close the reader
        in.close();
        
        // Print the result
        System.out.println(response.toString());
    } else {
        System.out.println("GET request failed. Response Code: " + responseCode);
    }
} catch (MalformedURLException e) {
    System.out.println("URL is malformed: " + e.getMessage());
} catch (IOException e) {
    System.out.println("Error during communication: " + e.getMessage());
}
*/