package org.cooking.cookingbenefits.config;

import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.addAllowedOrigin("*");
                    corsConfig.addAllowedMethod("*");
                    corsConfig.addAllowedHeader("*");
                    corsConfig.addExposedHeader("Authorization");
                    corsConfig.setAllowCredentials(false);
                    return corsConfig;
                }))
                .authorizeHttpRequests(auth -> auth
                        // Публичные endpoints (без аутентификации)
                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()

                        // Доступ к продуктам каталога - публичный
                        .requestMatchers(HttpMethod.GET, "/products/catalog").permitAll()

                        // Проверка доступности email - публичная
                        .requestMatchers(HttpMethod.GET, "/auth/check-email").permitAll()

                        // Только для администраторов
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Управление рецептами (добавление/удаление) - только админы
                        .requestMatchers(HttpMethod.POST, "/recipes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/recipes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/recipes/**").hasRole("ADMIN")

                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                {
                                    "error": "Unauthorized",
                                    "message": "Требуется аутентификация",
                                    "status": 401,
                                    "timestamp": "%s"
                                }
                                """.formatted(java.time.LocalDateTime.now())
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                {
                                    "error": "Forbidden",
                                    "message": "Недостаточно прав",
                                    "status": 403,
                                    "timestamp": "%s"
                                }
                                """.formatted(java.time.LocalDateTime.now())
                            );
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}