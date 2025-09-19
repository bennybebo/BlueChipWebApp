package com.bluechip.demo.controller;

import com.bluechip.demo.model.User;
import com.bluechip.demo.repositories.UserRepository;
import com.bluechip.demo.forms.SettingsForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserRepository users;

    public SettingsController(UserRepository users) {
        this.users = users;
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
        return "user_settings";
    }

}
