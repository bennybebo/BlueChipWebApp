package com.bluechip.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bluechip.demo.repositories.UserRepository;

@Controller
public class CalculatorController {

    private final UserRepository users;

    public CalculatorController(UserRepository users) {
        this.users = users;
    }

    // Handler for the Arbitrage Calculator page
    @GetMapping("/calculators/arbitrage")
    public String showArbitrageCalculator() {
        return "calculators/arbitrage";
    }
    
    // Handler for the Kelly Calculator page
    @GetMapping("/calculators/kelly")
    public String kelly(@AuthenticationPrincipal UserDetails principal, Model model) {
        double bankrollDollars = 0.0;
        double kellyFraction = 0.5;
        boolean fromSettings = false;

        if (principal != null) {
            var user = users.findByUsername(principal.getUsername());
            if (user != null) {
                bankrollDollars = user.getBankrollCents() / 100.0;
                kellyFraction = user.getPreferredKellyFraction();
                fromSettings = true;
            }
        }

        model.addAttribute("bankrollDollars", bankrollDollars);
        model.addAttribute("kellyFraction", kellyFraction);
        model.addAttribute("fromSettings", fromSettings);
        return "calculators/kelly";
    }
}
