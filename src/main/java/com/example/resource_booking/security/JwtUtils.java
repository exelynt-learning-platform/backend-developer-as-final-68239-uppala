package com.example.resource_booking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expirationMs:86400000}")
    private int jwtExpirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = resolveSigningKey();
    }

    private synchronized Key resolveSigningKey() {
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            String trimmed = jwtSecret.trim();

            // 1. Try Hex decode if it looks like a hex string (e.g. 64 hex characters for 256 bits)
            if (trimmed.matches("^[0-9a-fA-F]{64,}$")) {
                try {
                    byte[] hexBytes = decodeHex(trimmed);
                    return Keys.hmacShaKeyFor(hexBytes);
                } catch (Exception e) {
                    logger.debug("Hex decode failed, falling back: {}", e.getMessage());
                }
            }

            // 2. Try Base64 decode
            try {
                byte[] base64Bytes = Decoders.BASE64.decode(trimmed);
                if (base64Bytes.length >= 32) {
                    return Keys.hmacShaKeyFor(base64Bytes);
                }
            } catch (Exception e) {
                logger.debug("Base64 decode failed, falling back: {}", e.getMessage());
            }

            // 3. Fallback to raw UTF-8 bytes with SHA-256 derivation if < 32 bytes
            try {
                byte[] rawBytes = trimmed.getBytes(StandardCharsets.UTF_8);
                if (rawBytes.length >= 32) {
                    return Keys.hmacShaKeyFor(rawBytes);
                } else {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashed = md.digest(rawBytes);
                    return Keys.hmacShaKeyFor(hashed);
                }
            } catch (Exception e) {
                logger.error("Error creating key from secret string: {}", e.getMessage());
            }
        }

        logger.warn("JWT secret is not configured. Generating a secure random HMAC-SHA256 key for this session.");
        return Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    private byte[] decodeHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private Key key() {
        if (this.signingKey == null) {
            this.signingKey = resolveSigningKey();
        }
        return this.signingKey;
    }

    public String generateJwtToken(Authentication authentication) {
        com.example.resource_booking.security.services.UserDetailsImpl userPrincipal =
                (com.example.resource_booking.security.services.UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
