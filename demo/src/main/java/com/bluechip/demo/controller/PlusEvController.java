package com.bluechip.demo.controller;

import com.bluechip.demo.dto.PlusEvPickDto;
import com.bluechip.demo.model.User;
import com.bluechip.demo.repositories.UserRepository;
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

    public PlusEvController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails principal, Model model) {
        double bankroll = 0.0;
        double kelly    = 0.5;
        User user = null;

        if (principal != null) {
            user = users.findByUsername(principal.getUsername());
            if (user != null) {
                bankroll = user.getBankrollCents() / 100.0;
                kelly    = user.getPreferredKellyFraction();
            }
        }

        // --- Fake placeholder picks (so the table renders now) ---
        List<PlusEvPickDto> picks = new ArrayList<>();
        picks.add(buildPick(
            "Oct 10, 7:30 PM", "NFL", "All Sports",
            "Eagles", "Cowboys", "h2h", "HOME",
            0.56, +125, "DraftKings", bankroll, kelly));

        picks.add(buildPick(
            "Oct 10, 8:00 PM", "NBA", "All Sports",
            "Lakers", "Warriors", "h2h", "AWAY",
            0.52, +115, "FanDuel", bankroll, kelly));
        // ----------------------------------------------------------

        model.addAttribute("plusEvPicks", picks);
        model.addAttribute("sportName", "All Sports");
        model.addAttribute("bankrollDollars", bankroll);
        model.addAttribute("kellyFraction", kelly);

        // Optional for your existing partials; safe defaults:
        model.addAttribute("marketType", "h2h");
        model.addAttribute("sportKey", "all");
        model.addAttribute("availableSports", Collections.emptyList());
        model.addAttribute("user", user);

        return "plus_ev";
    }

    // Helpers

    private PlusEvPickDto buildPick(
            String time, String category, String sportName,
            String home, String away, String market, String outcome,
            double fairProb, int offeredAmerican, String book,
            double bankroll, double kellyFraction) {

        double offeredDecimal = decimalFromAmerican(offeredAmerican);
        double fairDecimal    = 1.0 / fairProb;

        double edgePct = (fairProb * offeredDecimal - 1.0) * 100.0; // %EV
        double stake   = kellyStake(bankroll, kellyFraction, fairProb, offeredDecimal);

        return PlusEvPickDto.builder()
                .formattedCommenceTime(time)
                .category(category)
                .sportName(sportName)
                .homeTeam(home)
                .awayTeam(away)
                .market(market)
                .outcome(outcome)
                .fairProb(fairProb)
                .fairDecimal(round2(fairDecimal))
                .bookmakerTitle(book)
                .offeredAmerican(offeredAmerican)
                .offeredDecimal(round2(offeredDecimal))
                .edgePct(round2(edgePct))
                .recommendedStakeDollars(round2(stake))
                .build();
    }

    private static double decimalFromAmerican(int a) {
        return a > 0 ? 1.0 + (a / 100.0) : 1.0 + (100.0 / Math.abs(a));
    }

    private static double kellyStake(double bankroll, double kellyFraction, double p, double d) {
        double b = d - 1.0;
        double q = 1.0 - p;
        double fStar = (b * p - q) / b;               // full Kelly fraction
        double f = Math.max(0.0, fStar) * kellyFraction;
        return bankroll * f;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

