package com.myfis.server.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 30)
    private String gender;

    @Column(nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false, length = 50)
    private String exerciseExperience;

    @Column(length = 80)
    private String referrer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(String name, String email, String passwordHash, Integer age, String gender,
                String phoneNumber, Integer height, Integer weight, String exerciseExperience,
                String referrer) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.age = age;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.height = height;
        this.weight = weight;
        this.exerciseExperience = exerciseExperience;
        this.referrer = referrer;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
}