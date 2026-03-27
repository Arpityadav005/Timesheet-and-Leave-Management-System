package com.tms.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"JWT_SECRET=01234567890123456789012345678901"
})
class AdminServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
