package com.bluechip.demo.config;

import com.bluechip.demo.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    public WebSecurityConfig(PasswordEncoder passwordEncoder, CustomUserDetailsService userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .authorizeHttpRequests(authz -> authz
                // Allow access to these URLs without authentication
                .requestMatchers(
                    "/", "/home", "/index", "/register", "/login",
                    "/css/**", "/js/**", "/images/**", "/assets/**", 
                    "/h2-console/**", "/public/**", "/webjars/**"
                ).permitAll()
                // Require authentication for these URLs
                .requestMatchers(
                    "/settings/**", "/updateBookmakers", "/user/**", "/preferences/**", "/secure/**"
                ).authenticated()
                // Any other request is permitted
                .anyRequest().permitAll()
            )
            // Configure form login
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/odds/americanfootball_nfl/h2h", true)
                .failureUrl("/login?error=true") // Redirect to /login with error on failure
                .permitAll()
            )
            // Configure logout
            .logout(logout -> logout
                .permitAll()
            )
            // Configure authentication provider
            .authenticationProvider(authenticationProvider())
            // Configure user details service
            .userDetailsService(userDetailsService);

        // Allow frames for H2 console (remove in production)
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        
        return authProvider;
    }
}
