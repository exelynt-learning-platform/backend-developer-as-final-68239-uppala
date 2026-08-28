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
import org.springframework.stereotype.Component;

import java.security.Key;
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

        try {
            byte[] secretBytes = Decoders.BASE64.decode(jwtSecret.trim());
            if (secretBytes.length < 32) {
                throw invalidSecretFormatException();
            }
            return Keys.hmacShaKeyFor(secretBytes);
        } catch (Exception ex) {
            throw invalidSecretFormatException();
        }
    }

    private IllegalStateException missingSecretException() {
        return new IllegalStateException(
                "JWT secret is required. Set JWT_SECRET to a stable secret of at least 32 bytes.");
    }

    private IllegalStateException invalidSecretFormatException() {
        return new IllegalStateException(
                "JWT secret must be a Base64-encoded key of at least 32 bytes.");
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
