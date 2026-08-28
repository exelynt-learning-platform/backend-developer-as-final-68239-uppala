package com.example.resource_booking.security;

import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private final int expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
    }

    @Test
    void generateAndValidateToken_Success() {
        User user = User.builder().id(1L).username("testuser").role(Role.USER).build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(auth);
        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("testuser", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void validateJwtToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtils.validateJwtToken("invalid.token.structure"));
    }

    @Test
    void missingSecret_ThrowsClearConfigurationError() {
        JwtUtils fallbackUtils = new JwtUtils();
        ReflectionTestUtils.setField(fallbackUtils, "jwtSecret", "");
        ReflectionTestUtils.setField(fallbackUtils, "jwtExpirationMs", expirationMs);

        User user = User.builder().id(2L).username("fallbackUser").role(Role.USER).build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fallbackUtils.generateJwtToken(auth));
        assertTrue(exception.getMessage().contains("JWT secret is required"));
    }

    @Test
    void plainTextSecret_ThrowsClearConfigurationError() {
        JwtUtils plainUtils = new JwtUtils();
        ReflectionTestUtils.setField(plainUtils, "jwtSecret", "mySimplePlainTextSecretKeyForTestingThatIsLongEnough");
        ReflectionTestUtils.setField(plainUtils, "jwtExpirationMs", expirationMs);

        IllegalStateException exception = assertThrows(IllegalStateException.class, plainUtils::init);
        assertTrue(exception.getMessage().contains("Base64-encoded"));
    }
}
