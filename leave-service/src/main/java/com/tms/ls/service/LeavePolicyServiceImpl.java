package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeavePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeavePolicyServiceImpl implements LeavePolicyService {
    private static final Logger log = LoggerFactory.getLogger(LeavePolicyServiceImpl.class);

    private final LeavePolicyRepository repository;

    public LeavePolicyServiceImpl(LeavePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeavePolicy> getAllPolicies() {
        log.debug("Loading all leave policies");
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LeavePolicy getPolicyByType(LeaveType type) {
        log.debug("Loading leave policy for type={}", type);
        return repository.findByLeaveType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found for type: " + type));
    }

    @Override
    @Transactional
    public LeavePolicy createOrUpdatePolicy(LeavePolicyDto dto) {
        log.info("Creating or updating leave policy for type={}", dto.getLeaveType());
        LeavePolicy policy = repository.findByLeaveType(dto.getLeaveType())
                .orElse(new LeavePolicy());
        
        policy.setLeaveType(dto.getLeaveType());
        policy.setDaysAllowed(dto.getDaysAllowed());
        policy.setCarryForwardAllowed(dto.isCarryForwardAllowed());
        policy.setMaxCarryForwardDays(dto.getMaxCarryForwardDays());
        policy.setRequiresDelegate(dto.isRequiresDelegate());
        if (dto.getMinDaysNotice() != null) {
            policy.setMinDaysNotice(dto.getMinDaysNotice());
        }
        policy.setActive(dto.isActive());
        
        LeavePolicy savedPolicy = repository.save(policy);
        log.info("Leave policy persisted id={} type={}", savedPolicy.getId(), savedPolicy.getLeaveType());
        return savedPolicy;
    }

    @Override
    @Transactional
    public void deletePolicy(String id) {
        log.info("Deleting leave policy id={}", id);
        LeavePolicy policy = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        repository.delete(policy);
    }
}
