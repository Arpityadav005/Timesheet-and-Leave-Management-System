package com.tms.admin.client.fallback;

import com.tms.admin.client.TimesheetServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TimesheetFallback implements TimesheetServiceClient {

    private static final Logger log = LoggerFactory.getLogger(TimesheetFallback.class);

    @Override
    public Object getTimesheetById(String id) {
        log.warn("Timesheet Service is unreachable for getTimesheetById fallback. TimesheetId: {}", id);
        return null;
    }

    @Override
    public void approveTimesheet(String id, java.util.Map<String, String> comments) {
        log.warn("Timesheet Service is unreachable for approveTimesheet fallback. TimesheetId: {}", id);
        throw new RuntimeException("Timesheet service is currently unavailable. Please try again later.");
    }

    @Override
    public void rejectTimesheet(String id, java.util.Map<String, String> comments) {
        log.warn("Timesheet Service is unreachable for rejectTimesheet fallback. TimesheetId: {}", id);
        throw new RuntimeException("Timesheet service is currently unavailable. Please try again later.");
    }
}
