package com.barstock.config;

import com.barstock.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email).map(u -> User.withUsername(u.getEmail())
                .password(u.getPasswordHash()).roles(u.getRole()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts) throws Exception {
        return http.securityContext(c -> c.securityContextRepository(contexts))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/api/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401); res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403); res.setContentType("application/json");
                            res.getWriter().write("{\"message\":\"Acceso denegado o sesión de formulario expirada\"}");
                        }))
                .logout(l -> l.disable()).build();
    }
}
