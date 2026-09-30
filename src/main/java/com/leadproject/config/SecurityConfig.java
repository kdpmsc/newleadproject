package com.leadproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AppProperties appProperties;
    private final CorsProperties corsProperties;
    private final SecurityProperties securityProperties;

    public SecurityConfig(AppProperties appProperties,
                          CorsProperties corsProperties,
                          SecurityProperties securityProperties) {
        this.appProperties = appProperties;
        this.corsProperties = corsProperties;
        this.securityProperties = securityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<UserDetails> users = new ArrayList<>();
        if (securityProperties.getUsers() != null) {
            for (SecurityProperties.UserAccount account : securityProperties.getUsers()) {
                if (account.getUsername() == null || account.getUsername().isBlank()) {
                    continue;
                }
                if (account.getPassword() == null || account.getPassword().isBlank()) {
                    logger.warn("Skipping security user '{}': password not set (use env / .env)",
                            account.getUsername());
                    continue;
                }
                String[] roles = account.getRoles() == null || account.getRoles().isEmpty()
                        ? new String[]{"USER"}
                        : account.getRoles().toArray(String[]::new);
                users.add(User.withUsername(account.getUsername().trim())
                        .password(passwordEncoder.encode(account.getPassword()))
                        .roles(roles)
                        .build());
            }
        }
        if (users.isEmpty()) {
            logger.warn("No in-memory security users configured. Set APP_ADMIN_PASSWORD (and optional "
                    + "APP_SALES_PASSWORD / APP_COMPLIANCE_PASSWORD) via environment / .env");
        }
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api", "/index.html", "/app.js", "/styles.css",
                                "/dashboard.html", "/sales-brief.html", "/excel-template.csv",
                                "/actuator/health", "/error", "/api/v1/auth/**",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                                "/api/v1/voice/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.resolvedAllowedOrigins(appProperties.getFrontendOrigin()));
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}