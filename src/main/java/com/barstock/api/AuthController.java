package com.barstock.api;

import com.barstock.model.AppUser;
import com.barstock.repository.UserRepository;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contexts;
    private final String dummyHash;
    public AuthController(UserRepository users, PasswordEncoder encoder, SecurityContextRepository contexts) {
        this.users = users; this.encoder = encoder; this.contexts = contexts;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    @GetMapping("/auth/csrf") public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterInput input, HttpServletRequest request, HttpServletResponse response) {
        validatePassword(input.password());
        String email = normalize(input.email());
        if (users.findByEmail(email).isPresent()) throw conflict();
        AppUser user = new AppUser(); user.setEmail(email); user.setName(input.name().trim());
        user.setPasswordHash(encoder.encode(input.password())); user.setRole("USER");
        try { user = users.saveAndFlush(user); } catch (DataIntegrityViolationException ex) { throw conflict(); }
        return signIn(user, request, response);
    }
    @PostMapping("/auth/login") public UserView login(@Valid @RequestBody LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        AppUser user = users.findByEmail(normalize(input.email())).orElse(null);
        boolean valid = encoder.matches(input.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        return signIn(user, request, response);
    }
    @GetMapping("/auth/me") public UserView me(Authentication auth) { return view(current(auth)); }
    @PostMapping("/auth/logout") public Map<String, String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); if (session != null) session.invalidate();
        SecurityContextHolder.clearContext(); return Map.of("message", "Sesión cerrada");
    }
    @PostMapping("/auth/password") public Map<String, String> password(@Valid @RequestBody PasswordInput input, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        AppUser user = current(auth);
        if (!encoder.matches(input.currentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
        validatePassword(input.newPassword()); user.setPasswordHash(encoder.encode(input.newPassword())); users.save(user);
        signIn(user, request, response); return Map.of("message", "Contraseña actualizada");
    }
    @GetMapping("/admin/users") public List<UserView> listUsers() { return users.findAll().stream().map(this::view).toList(); }
    private UserView signIn(AppUser user, HttpServletRequest req, HttpServletResponse res) {
        req.getSession(); req.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
        SecurityContextHolder.setContext(context); contexts.saveContext(context, req, res);
        return view(user);
    }
    private AppUser current(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    private UserView view(AppUser u) { return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole()); }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes");
    }
    private ResponseStatusException conflict() { return new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado"); }
    public record UserView(Long id, String name, String email, String role) {}
    public record RegisterInput(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=180) String email,
            @NotBlank @Size(min=12, max=72) String password) {}
    public record LoginInput(@NotBlank @Email @Size(max=180) String email, @NotBlank @Size(max=72) String password) {}
    public record PasswordInput(@NotBlank @Size(max=72) String currentPassword, @NotBlank @Size(min=12, max=72) String newPassword) {}
}
