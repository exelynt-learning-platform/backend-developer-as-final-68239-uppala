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
    private final String secret = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
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
    void knownInsecureDefaultSecret_ThrowsClearConfigurationError() {
        JwtUtils insecureUtils = new JwtUtils();
        ReflectionTestUtils.setField(insecureUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(insecureUtils, "jwtExpirationMs", expirationMs);

        User user = User.builder().id(2L).username("user").role(Role.USER).build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> insecureUtils.generateJwtToken(auth));
        assertTrue(exception.getMessage().contains("Insecure default JWT secret detected"));
    }

    @Test
    void testPlainTextSecretKey() {
        JwtUtils plainUtils = new JwtUtils();
        ReflectionTestUtils.setField(plainUtils, "jwtSecret", "mySimplePlainTextSecretKeyForTestingThatIsLongEnough");
        ReflectionTestUtils.setField(plainUtils, "jwtExpirationMs", expirationMs);

        User user = User.builder().id(3L).username("plainUser").role(Role.USER).build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        String token = plainUtils.generateJwtToken(auth);
        assertNotNull(token);
        assertTrue(plainUtils.validateJwtToken(token));
        assertEquals("plainUser", plainUtils.getUserNameFromJwtToken(token));
    }
}
