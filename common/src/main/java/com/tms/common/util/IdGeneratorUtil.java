package com.tms.common.util;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IdGeneratorUtil {

    public String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}