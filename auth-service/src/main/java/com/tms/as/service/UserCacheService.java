package com.tms.as.service;

import com.tms.as.entity.User;
import com.tms.as.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserCacheService {
    private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

    private final UserRepository userRepository;

    public UserCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "usersByEmail", key = "#email")
    public User getUserByEmail(String email) {
        log.debug("Loading user from cache-backed lookup for email={}", email);
        return userRepository.findByEmail(email).orElse(null);
    }
}
