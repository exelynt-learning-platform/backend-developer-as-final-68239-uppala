package com.example.resource_booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "app.seed.admin-password=test-admin-password",
        "app.seed.user-password=test-user-password"
})
class ResourceBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
