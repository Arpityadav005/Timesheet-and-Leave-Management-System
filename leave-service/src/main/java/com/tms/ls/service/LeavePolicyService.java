package com.tms.ls.service;

import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import java.util.List;

public interface LeavePolicyService {
    List<LeavePolicy> getAllPolicies();
    LeavePolicy getPolicyByType(LeaveType type);
    LeavePolicy createOrUpdatePolicy(LeavePolicyDto dto);
    void deletePolicy(String id);
}
