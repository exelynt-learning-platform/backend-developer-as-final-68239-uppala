package com.example.resource_booking.controller;

import com.example.resource_booking.dto.JwtResponse;
import com.example.resource_booking.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=NDBFNjM1MjY2NTU2QTU4NkUzMjcyMzU3NTM4NzhGMkY0MUYzNDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
                "app.seed.admin-password=test-admin-password",
                "app.seed.user-password=user123"
        }
)
public class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void unauthenticatedRequest_ReturnsUnauthorized() {
        try {
            restTemplate.getForEntity("http://localhost:" + port + "/api/resources", String.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void userRequestingAdminEndpoint_ReturnsForbidden() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user");
        loginRequest.setPassword("user123");
        
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginRequest, JwtResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String token = loginResponse.getBody().getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange("http://localhost:" + port + "/api/resources/1", HttpMethod.DELETE, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void invalidJwtToken_ReturnsUnauthorizedAndAborts() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.tampered.jwttoken");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange("http://localhost:" + port + "/api/resources", HttpMethod.GET, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }
}
