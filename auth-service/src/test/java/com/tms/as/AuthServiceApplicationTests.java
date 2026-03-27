package com.tms.as;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"app.jwt.secret=01234567890123456789012345678901",
		"app.jwt.expiration-ms=3600000"
})
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
