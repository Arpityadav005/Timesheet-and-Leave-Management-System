package com.tms.ls.controller;

import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.service.LeavePolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyControllerTest {

    @Mock
    private LeavePolicyService leavePolicyService;

    @InjectMocks
    private LeavePolicyController controller;

    @Test
    void getAllPolicies_ReturnsOkResponse() {
        LeavePolicy policy = new LeavePolicy();
        policy.setId("POL-1");
        when(leavePolicyService.getAllPolicies()).thenReturn(List.of(policy));

        ResponseEntity<List<LeavePolicy>> response = controller.getAllPolicies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPolicyByType_ReturnsOkResponse() {
        LeavePolicy policy = new LeavePolicy();
        policy.setLeaveType(LeaveType.CASUAL);
        when(leavePolicyService.getPolicyByType(LeaveType.CASUAL)).thenReturn(policy);

        ResponseEntity<LeavePolicy> response = controller.getPolicyByType(LeaveType.CASUAL);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(LeaveType.CASUAL, response.getBody().getLeaveType());
    }

    @Test
    void createOrUpdatePolicy_ReturnsCreatedResponse() {
        LeavePolicyDto request = new LeavePolicyDto();
        request.setLeaveType(LeaveType.ANNUAL);
        request.setDaysAllowed(BigDecimal.valueOf(12));

        LeavePolicy policy = new LeavePolicy();
        policy.setId("POL-1");

        when(leavePolicyService.createOrUpdatePolicy(request)).thenReturn(policy);

        ResponseEntity<LeavePolicy> response = controller.createOrUpdatePolicy(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("POL-1", response.getBody().getId());
    }

    @Test
    void deletePolicy_ReturnsNoContentAndDelegates() {
        ResponseEntity<Void> response = controller.deletePolicy("POL-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(leavePolicyService).deletePolicy("POL-1");
    }
}
