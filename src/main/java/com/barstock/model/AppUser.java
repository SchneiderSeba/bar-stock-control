package com.barstock.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(nullable = false, length = 120) private String name;
    @JsonIgnore @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 20) private String role = "USER";
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
