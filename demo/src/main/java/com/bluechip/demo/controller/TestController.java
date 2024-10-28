package com.bluechip.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/index")
    public String showLoginForm() {
        return "index"; // This corresponds to login.html in your templates directory
    }
}