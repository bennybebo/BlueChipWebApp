package com.bluechip.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bluechip.demo.model.Odds;
import com.bluechip.demo.service.OddsService;

import java.io.IOException;
import java.util.List;

@Controller
public class OddsController {

    @Autowired
    private OddsService oddsService;

    // Render the HTML page with odds
    @GetMapping("/odds")
    public String showOdds(Model model) throws IOException {
        List<Odds> oddsList = oddsService.readOddsFromFile();
        model.addAttribute("oddsList", oddsList);  // Add the odds list to the model
        return "odds";  // Return the template name (odds.html)
    }
}
