package com.example.resource_booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=NDBFNjM1MjY2NTU2QTU4NkUzMjcyMzU3NTM4NzhGMkY0MUYzNDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
        "app.seed.admin-password=test-admin-password",
        "app.seed.user-password=test-user-password"
})
class ResourceBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
