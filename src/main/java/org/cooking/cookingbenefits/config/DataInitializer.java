package org.cooking.cookingbenefits.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("База пользователей пуста – добавляем начальные данные...");

                userRepository.save(User.builder()
                        .email("user1@gmail.com")
                        .passwordHash(passwordEncoder.encode("user1Pass"))
                        .fullName("User One")
                        .role("USER")
                        .isActive(true)
                        .build());

                userRepository.save(User.builder()
                        .email("user2@gmail.com")
                        .passwordHash(passwordEncoder.encode("user2Pass"))
                        .fullName("User Two")
                        .role("USER")
                        .isActive(true)
                        .build());

                userRepository.save(User.builder()
                        .email("admin@gmail.com")
                        .passwordHash(passwordEncoder.encode("adminPass"))
                        .fullName("Administrator")
                        .role("ADMIN")
                        .isActive(true)
                        .build());

                log.info("Тестовые пользователи созданы (включая администратора).");
            } else {
                log.info("База пользователей не пуста – пропускаем инициализацию.");
            }
        };
    }
}
