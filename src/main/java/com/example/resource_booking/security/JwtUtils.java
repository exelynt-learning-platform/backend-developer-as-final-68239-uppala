package com.example.resource_booking.security;

import com.example.resource_booking.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.codec.Hex;
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
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw missingSecretException();
        }

        String secret = jwtSecret.trim();
        Key hexKey = decodeHexKey(secret);
        if (hexKey != null) {
            return hexKey;
        }

        Key base64Key = decodeBase64Key(secret);
        if (base64Key != null) {
            return base64Key;
        }

        Key rawKey = deriveRawSecretKey(secret);
        if (rawKey != null) {
            return rawKey;
        }

        throw missingSecretException();
    }

    private Key decodeHexKey(String secret) {
        if (!secret.matches("^[0-9a-fA-F]{64,}$")) {
            return null;
        }
        try {
            return Keys.hmacShaKeyFor(Hex.decode(secret));
        } catch (Exception e) {
            logger.debug("Hex decode failed, falling back: {}", e.getMessage());
            return null;
        }
    }

    private Key decodeBase64Key(String secret) {
        try {
            byte[] base64Bytes = Decoders.BASE64.decode(secret);
            return base64Bytes.length >= 32 ? Keys.hmacShaKeyFor(base64Bytes) : null;
        } catch (Exception e) {
            logger.debug("Base64 decode failed, falling back: {}", e.getMessage());
            return null;
        }
    }

    private Key deriveRawSecretKey(String secret) {
        try {
            byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (rawBytes.length >= 32) {
                return Keys.hmacShaKeyFor(rawBytes);
            }
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(rawBytes);
            return Keys.hmacShaKeyFor(hashed);
        } catch (Exception e) {
            logger.error("Error creating key from secret string: {}", e.getMessage());
            return null;
        }
    }

    private IllegalStateException missingSecretException() {
        return new IllegalStateException(
                "JWT secret is required. Set JWT_SECRET to a stable secret of at least 32 bytes.");
    }

    private Key key() {
        if (this.signingKey == null) {
            this.signingKey = resolveSigningKey();
        }
        return this.signingKey;
    }

    public String generateJwtToken(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal)) {
            throw new AuthenticationServiceException("Unsupported authenticated principal");
        }

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
