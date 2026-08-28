package com.example.resource_booking.config;

import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);

            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
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
