package com.bluechip.demo.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Getter @Setter
    private String username;

    // Column name is 'password' in DB
    @Column(name = "password", nullable = false, length = 100)
    @Getter @Setter
    private String password;

    // Comma-separated roles, e.g., "ROLE_USER" or "ROLE_USER,ROLE_PREMIUM"
    @Column(nullable = false, length = 200)
    @Getter @Setter
    private String roles = "ROLE_USER";

    @Column(name = "bankroll_cents", nullable = false)
    @Getter @Setter
    private long bankrollCents = 0L;

    @Column(name = "preferred_kelly_fraction", nullable = false)
    @Getter @Setter
    private double preferredKellyFraction = 0.5;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // Constructors
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
