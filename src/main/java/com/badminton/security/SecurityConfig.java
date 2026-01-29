package com.badminton.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth
                        // ✅ PayOS endpoints
                        .requestMatchers("/payments/payos/webhook").permitAll()
                        .requestMatchers("/payments/payos/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payments/payos/status/**").permitAll()

                        // ✅ Existing public endpoints
                        .requestMatchers(
                                "/auth/**",
                                "/payments/momo/webhook",
                                "/payments/mock/**",
                                "/shop/payments/momo/webhook",
                                "/shop/payments/mock/**",
                                "/payments/vnpay/callback",
                                "/error")
                        .permitAll()

                        // ✅ Shop public read endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/shop/categories/**",
                                "/shop/products/**",
                                "/shop/reviews/product/**",
                                "/shop/reviews/latest-verified")
                        .permitAll()

                        // ✅ Courts public read
                        .requestMatchers(HttpMethod.GET, "/courts/**").permitAll()

                        // ✅ Courts authenticated write
                        .requestMatchers(HttpMethod.POST, "/courts").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/courts/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/courts/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/courts/**").hasAnyRole("ADMIN", "OWNER")

                        .requestMatchers("/chat/**").authenticated()
                        .requestMatchers("/location/**").authenticated()
                        .requestMatchers("/user-tier/**").authenticated()
                        .requestMatchers("/qr/**").authenticated()

                        .requestMatchers("/health", "/error").permitAll()
                        // ✅ Everything else needs auth
                        .anyRequest().authenticated())

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Cho phép tất cả origins (development)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // ✅ Hoặc chỉ định cụ thể (production)
        // configuration.setAllowedOrigins(Arrays.asList(
        // "http://localhost:8081",
        // "http://localhost:19006",
        // "https://your-frontend-domain.com"
        // ));

        // ✅ Cho phép tất cả HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // ✅ Cho phép tất cả headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ✅ Expose Authorization header
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // ✅ Cho phép credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // ✅ Cache preflight request trong 1 giờ
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
