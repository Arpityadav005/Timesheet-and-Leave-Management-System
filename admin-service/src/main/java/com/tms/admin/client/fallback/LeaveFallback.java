package com.tms.admin.client.fallback;

import com.tms.admin.client.LeaveServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LeaveFallback implements LeaveServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(LeaveFallback.class);

    @Override
    public Object getLeaveRequestById(String id) {
        log.warn("Leave Service is unreachable for getLeaveRequestById fallback. RequestId: {}", id);
        return null; // Empty object on fallback
    }

    @Override
    public void approveLeave(String id, java.util.Map<String, String> comments) {
        log.warn("Leave Service is unreachable for approveLeave fallback. RequestId: {}", id);
        throw new RuntimeException("Leave service is currently unavailable. Please try again later.");
    }

    @Override
    public void rejectLeave(String id, java.util.Map<String, String> comments) {
        log.warn("Leave Service is unreachable for rejectLeave fallback. RequestId: {}", id);
        throw new RuntimeException("Leave service is currently unavailable. Please try again later.");
    }
}
