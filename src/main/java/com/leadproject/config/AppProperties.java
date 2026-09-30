package com.leadproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Public HTTPS base URL Twilio/Plivo call for webhooks
     * (ngrok URL, server URL). Prefer APP_PUBLIC_BASE_URL; TWILIO_APP_BASE_URL is aliased.
     * Default lives in application.yml — do not hardcode here.
     */
    private String publicBaseUrl;

    /** SPA / static UI origin used for CORS (FRONTEND_ORIGIN). */
    private String frontendOrigin;

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getFrontendOrigin() { return frontendOrigin; }
    public void setFrontendOrigin(String frontendOrigin) { this.frontendOrigin = frontendOrigin; }

    public String trimmedPublicBaseUrl() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "";
        }
        return publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }
}