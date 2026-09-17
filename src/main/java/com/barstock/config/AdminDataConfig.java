package com.barstock.config;

import com.barstock.model.AppUser;
import com.barstock.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;

@Configuration
public class AdminDataConfig {
    @Bean CommandLineRunner adminUser(UserRepository users, PasswordEncoder encoder,
            @Value("${ADMIN_EMAIL:admin@barstock.app}") String email,
            @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (users.findByEmail(normalized).isPresent()) return;
            if (password.isBlank()) return;
            if (password.length() < 12 || password.length() > 72)
                throw new IllegalArgumentException("ADMIN_PASSWORD debe tener entre 12 y 72 caracteres");
            AppUser user = new AppUser(); user.setEmail(normalized); user.setName("Administrador");
            user.setPasswordHash(encoder.encode(password)); user.setRole("ADMIN"); users.save(user);
        };
    }
}
