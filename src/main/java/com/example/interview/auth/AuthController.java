package com.example.interview.auth;

import com.example.interview.security.JwtService;
import com.example.interview.user.UserAccount;
import com.example.interview.user.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(
            UserAccountRepository userRepo,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authManager,
            JwtService jwtService
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email is already registered");
            return ResponseEntity.badRequest().body(error);
        }

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        UserAccount user = new UserAccount();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);

        userRepo.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("fullName", user.getFullName());

        String token = jwtService.generateToken(user.getEmail(), claims);

        AuthResponse resp = new AuthResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setFullName(user.getFullName());
        resp.setEmail(user.getEmail());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserAccount user = userRepo.findByEmail(request.getEmail())
                    .orElseThrow();

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("fullName", user.getFullName());

            String token = jwtService.generateToken(user.getEmail(), claims);

            AuthResponse resp = new AuthResponse();
            resp.setToken(token);
            resp.setUserId(user.getId());
            resp.setFullName(user.getFullName());
            resp.setEmail(user.getEmail());

            return ResponseEntity.ok(resp);
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }
    }
}
