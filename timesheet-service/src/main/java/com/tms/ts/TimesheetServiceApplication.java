package com.tms.ts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableCaching
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"com.tms.ts", "com.tms.common"})
public class TimesheetServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimesheetServiceApplication.class, args);
    }
}
