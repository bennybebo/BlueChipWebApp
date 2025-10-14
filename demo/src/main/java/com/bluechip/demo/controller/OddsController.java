package com.bluechip.demo.controller;

import com.bluechip.demo.dto.FairPriceDto;
import com.bluechip.demo.model.*;
import com.bluechip.demo.repositories.UserRepository;
import com.bluechip.demo.service.OddsProcessingService;
import com.bluechip.demo.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/odds")
public class OddsController {

    @Autowired
    private OddsService oddsService;

    @Autowired
    private OddsProcessingService oddsProcessingService;

    private final UserRepository userRepository;

    public OddsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{sport}/{market}")
    public String getOdds(
            @PathVariable("sport") String sportKey,
            @PathVariable("market") String marketType,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {
        try {
            // Fetch odds data, which will check for existing JSON file or fetch from API
            OddsProcessingService.ProcessedOddsResult result =
                    oddsProcessingService.processAndStore(sportKey, marketType);

            List<Odds> oddsList = result.oddsList();

            // Get unique bookmakers offering the specified market type
            Set<Bookmaker> uniqueBookmakers = oddsService.getUniqueBookmakers(oddsList, marketType);

            // Map matchups to their corresponding bookmakers and odds
            Map<String, Map<String, Bookmaker>> matchupBookmakerMap = oddsService.getMatchupBookmakerMap(oddsList, marketType);

            // Map sportKey to a user-friendly sport name
            String sportName = oddsService.getSportName(sportKey);

            // Get the list of available sports
            List<Map<String, String>> availableSports = oddsService.getAvailableSports();

            // Fair price map (already computed during processing)
            Map<String, FairPriceDto> fairMap = result.fairPrices();

            // === NEW: role flag (Premium vs Free) ===
            boolean isPremium = false;
            if (principal != null) {
                User u = userRepository.findByUsername(principal.getUsername());
                if (u != null && u.getRoles() != null && u.getRoles().contains("ROLE_PREMIUM")) {
                    isPremium = true;
                }
            }

            // Add attributes to the model for use in the view
            model.addAttribute("oddsList", oddsList);
            model.addAttribute("uniqueBookmakers", uniqueBookmakers);
            model.addAttribute("matchupBookmakerMap", matchupBookmakerMap);
            model.addAttribute("sportKey", sportKey);
            model.addAttribute("marketType", marketType);
            model.addAttribute("sportName", sportName);
            model.addAttribute("availableSports", availableSports);
            model.addAttribute("fairMap", fairMap);
            model.addAttribute("isPremium", isPremium);
            model.addAttribute("refreshedAt", result.refreshedAt());
            model.addAttribute("didRefresh", result.refreshed());

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Unable to load odds data.");
        }
        // Return the view corresponding to the market type
        return "odds_" + marketType;
    }

}
