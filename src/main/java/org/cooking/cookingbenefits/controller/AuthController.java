package org.cooking.cookingbenefits.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.AuthRequest;
import org.cooking.cookingbenefits.dto.AuthResponse;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.repository.UserRepository;
import org.cooking.cookingbenefits.security.JwtTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        // Аутентификация
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(), // Используем email
                        authRequest.getPassword()
                )
        );

        // Генерация токена
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenUtil.generateToken(userDetails);

        // Получение пользователя
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Обновление времени последнего входа
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Используем конструктор напрямую
        return ResponseEntity.ok(new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest authRequest) {
        // Проверка существования пользователя
        if (userRepository.existsByEmail(authRequest.getEmail())) {
            return ResponseEntity.badRequest().body("{\"error\": \"Email уже используется\"}");
        }

        // Создание нового пользователя
        User user = new User();
        user.setEmail(authRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(authRequest.getPassword()));
        user.setFullName(authRequest.getFullName());
        user.setIsActive(true);
        user.setRole("user");

        userRepository.save(user);

        // Автоматический логин после регистрации
        return login(authRequest);
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);
        return ResponseEntity.ok().body("{\"exists\": " + exists + "}");
    }
}