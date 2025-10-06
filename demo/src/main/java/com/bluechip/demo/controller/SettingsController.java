package com.bluechip.demo.controller;

import com.bluechip.demo.model.User;
import com.bluechip.demo.repositories.UserRepository;
import com.bluechip.demo.service.CustomUserDetailsService;
import com.bluechip.demo.forms.SettingsForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserRepository users;

    private final CustomUserDetailsService userDetailsService;
    
    public SettingsController(UserRepository users, CustomUserDetailsService userDetailsService) {
        this.users = users;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping()
    public String view(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = users.findByUsername(principal.getUsername());
        if (user == null) {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "User not found: " + principal.getUsername());
        }

        var form = new SettingsForm();
        form.setBankrollDollars(user.getBankrollCents() / 100.0);
        form.setPreferredKellyFraction(user.getPreferredKellyFraction());

        model.addAttribute("form", form);
        model.addAttribute("isPremium", hasRole(user.getRoles(), "ROLE_PREMIUM"));
        return "user_settings"; // ← matches your template filename
    }

    @PostMapping()
    public String save(@AuthenticationPrincipal UserDetails principal,
                       @Valid @ModelAttribute("form") SettingsForm form,
                       BindingResult errors,
                       Model model) {
        if (errors.hasErrors()) return "user_settings";

        User user = users.findByUsername(principal.getUsername());
        if (user == null) {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "User not found: " + principal.getUsername());
        }

        long cents = form.getBankrollDollars() == null ? 0L
                : Math.max(0L, Math.round(form.getBankrollDollars() * 100));
        double kelly = form.getPreferredKellyFraction() == null ? 0.5
                : form.getPreferredKellyFraction();

        user.setBankrollCents(cents);
        user.setPreferredKellyFraction(kelly);
        users.save(user);

        model.addAttribute("success", true);
        model.addAttribute("isPremium", hasRole(user.getRoles(), "ROLE_PREMIUM"));
        return "user_settings";
    }

    @PostMapping("/toggle-premium")
    public String togglePremium(@AuthenticationPrincipal UserDetails principal,
                                RedirectAttributes ra) {
        User user = users.findByUsername(principal.getUsername());
        if (user == null) {
            ra.addFlashAttribute("error", "User not found: " + principal.getUsername());
            return "redirect:/settings";
        }

        String updatedRoles = togglePremiumRole(user.getRoles());
        user.setRoles(updatedRoles);
        users.save(user);

        var updated = userDetailsService.loadUserByUsername(user.getUsername());
        var newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                updated, updated.getPassword(), updated.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);
        boolean nowPremium = hasRole(updatedRoles, "ROLE_PREMIUM");
        ra.addFlashAttribute("success", nowPremium ? "Premium enabled." : "Premium disabled.");
        return "redirect:/settings";
    }

    // ===== helpers =====

    private static boolean hasRole(String roles, String role) {
        if (roles == null || roles.isBlank()) return false;
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    private static String togglePremiumRole(String roles) {
        // normalize to a distinct, trimmed list
        List<String> list = (roles == null || roles.isBlank())
                ? new ArrayList<>()
                : Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new));

        // ensure ROLE_USER exists
        if (list.stream().noneMatch("ROLE_USER"::equals)) {
            list.add("ROLE_USER");
        }

        if (list.stream().anyMatch("ROLE_PREMIUM"::equals)) {
            // remove premium
            list = list.stream()
                    .filter(r -> !r.equals("ROLE_PREMIUM"))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            // add premium
            list.add("ROLE_PREMIUM");
        }

        // dedupe and join
        return list.stream().distinct().collect(Collectors.joining(","));
    }
}
