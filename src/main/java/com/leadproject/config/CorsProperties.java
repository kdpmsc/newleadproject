package com.leadproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    /** Comma-separated extra origins from env (CORS_ALLOWED_ORIGINS), merged with allowedOrigins. */
    private String extraAllowedOrigins = "";
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();
    private boolean allowCredentials = true;

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    public String getExtraAllowedOrigins() { return extraAllowedOrigins; }
    public void setExtraAllowedOrigins(String extraAllowedOrigins) { this.extraAllowedOrigins = extraAllowedOrigins; }
    public List<String> getAllowedOriginPatterns() { return allowedOriginPatterns; }
    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) { this.allowedOriginPatterns = allowedOriginPatterns; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
    public List<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public boolean isAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

    public List<String> resolvedAllowedOrigins(String frontendOrigin) {
        Set<String> origins = new LinkedHashSet<>();
        if (allowedOrigins != null) {
            allowedOrigins.stream().map(String::trim).filter(s -> !s.isBlank()).forEach(origins::add);
        }
        if (frontendOrigin != null && !frontendOrigin.isBlank()) {
            origins.add(frontendOrigin.trim());
        }
        if (extraAllowedOrigins != null && !extraAllowedOrigins.isBlank()) {
            Arrays.stream(extraAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(origins::add);
        }
        return List.copyOf(origins);
    }
}