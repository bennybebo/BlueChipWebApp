    package com.bluechip.demo.controller;

import com.bluechip.demo.dto.PlusEvPickDto;
import com.bluechip.demo.model.Bookmaker;
import com.bluechip.demo.model.User;
import com.bluechip.demo.repositories.UserRepository;
import com.bluechip.demo.service.PlusEvService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/plus-ev")
public class PlusEvController {

    private final UserRepository users;
    private final PlusEvService plusEvService;

    public PlusEvController(UserRepository users, PlusEvService plusEvService) {
        this.users = users;
        this.plusEvService = plusEvService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails principal, Model model) {
        double bankroll = 0.0;
        double kelly = 0.5;
        User user = null;

        if (principal != null) {
            user = users.findByUsername(principal.getUsername());
            if (user != null) {
                bankroll = user.getBankrollCents() / 100.0;
                kelly    = user.getPreferredKellyFraction();
            }
        }

        List<PlusEvPickDto> picks = plusEvService.loadPlusEvPicks(bankroll, kelly);
        List<Bookmaker> uniqueBookmakers = plusEvService.deriveBookmakers(picks);

        model.addAttribute("plusEvPicks", picks);
        model.addAttribute("sportName", "All Sports");
        model.addAttribute("bankrollDollars", bankroll);
        model.addAttribute("kellyFraction", kelly);
        model.addAttribute("uniqueBookmakers", uniqueBookmakers);

        // Optional for your existing partials; safe defaults:
        model.addAttribute("marketType", "h2h");
        model.addAttribute("sportKey", "all");
        model.addAttribute("availableSports", Collections.emptyList());
        model.addAttribute("user", user);

        return "plus_ev";
    }
}

