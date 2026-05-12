package com.hopesapms.app.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class VerificationService {

    private final Map<String, LocalDateTime> verifiedEmailCache = new ConcurrentHashMap<>();

    private static final int EXPIRATION_MINUTES = 10;

    public void cacheVerifiedEmail(String email) {
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        verifiedEmailCache.put(email, expiration);
        log.info("Cached verified email {} for profile completion. Expires at: {}", email, expiration);
    }

    public boolean validateVerifiedEmail(String email) {
        LocalDateTime expiration = verifiedEmailCache.get(email);

        if (expiration == null) {
            log.warn("No verified email cache found for {}", email);
            return false;
        }

        if (expiration.isBefore(LocalDateTime.now())) {
            log.warn("Verified email cache expired for {}", email);
            verifiedEmailCache.remove(email);
            return false;
        }

        verifiedEmailCache.remove(email);
        log.info("Successfully validated and consumed verified email cache for {}", email);
        return true;
    }
}