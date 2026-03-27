package com.tms.gateaway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.tms.gateway.ApiGatewayApplication;

@SpringBootTest(classes = ApiGatewayApplication.class, properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"JWT_SECRET=01234567890123456789012345678901"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
