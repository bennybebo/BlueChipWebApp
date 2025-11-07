package com.bluechip.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.bluechip.demo.service.OddsService;

@SpringBootApplication  // or use your existing main app class
public class OddsDebugLauncher {

    public static void main(String[] args) throws Exception {
        // Start Spring and load the context
        ApplicationContext ctx = SpringApplication.run(OddsDebugLauncher.class, args);

        // Grab your service bean
        OddsService oddsService = ctx.getBean(OddsService.class);

        // Call your target method
        long snapshotId = oddsService.refreshSportSnapshot("americanfootball_nfl", "h2h");

        System.out.println("Finished refreshSportSnapshot(), snapshotId = " + snapshotId);
    }
}
