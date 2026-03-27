package com.tms.admin.repository;

import com.tms.admin.entity.PolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyConfigRepository extends JpaRepository<PolicyConfig, String> {
    Optional<PolicyConfig> findByConfigKey(String configKey);
}
