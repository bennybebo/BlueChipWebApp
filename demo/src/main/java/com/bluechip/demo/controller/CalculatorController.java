package com.bluechip.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CalculatorController {

    // Handler for the Arbitrage Calculator page
    @GetMapping("/calculators/arbitrage")
    public String showArbitrageCalculator() {
        return "calculators/arbitrage";
    }

    // Handler for the Kelly Calculator page
    @GetMapping("/calculators/kelly")
    public String showKellyCalculator() {
        return "calculators/kelly";
    }
}
