package com.leadproject.config;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "voice.provider", havingValue = "twilio", matchIfMissing = true)
public class TwilioConfig {

    private static final Logger logger = LoggerFactory.getLogger(TwilioConfig.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            logger.warn("Twilio credentials are not configured. Outbound calls will fail until "
                    + "TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN are set.");
            return;
        }

        Twilio.init(accountSid, authToken);
        logger.info("Twilio client initialized");
    }
}
