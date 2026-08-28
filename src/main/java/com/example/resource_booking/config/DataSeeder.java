package com.example.resource_booking.config;

import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;

    public DataSeeder(UserRepository userRepository,
                      ResourceRepository resourceRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.user-password}") String userPassword) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);

            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode(userPassword))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
        }

        if (resourceRepository.count() == 0) {
            Resource r1 = Resource.builder()
                    .name("Conference Room A")
                    .type("Room")
                    .description("Large conference room with projector")
                    .build();
            resourceRepository.save(r1);

            Resource r2 = Resource.builder()
                    .name("Projector Model X")
                    .type("Equipment")
                    .description("HD Projector")
                    .build();
            resourceRepository.save(r2);
        }
    }
}
