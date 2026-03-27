package com.tms.cs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.profiles.active=native",
		"spring.cloud.config.server.native.search-locations=classpath:/test-config",
		"eureka.client.enabled=false",
		"spring.cloud.config.enabled=false"
})
class ConfigServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
