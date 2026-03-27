package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeavePolicyRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceImplTest {

    @Mock
    private LeavePolicyRepository repository;

    @InjectMocks
    private LeavePolicyServiceImpl service;

    @Test
    void getAllPolicies_ReturnsRepositoryResults() {
        LeavePolicy policy = new LeavePolicy();
        when(repository.findAll()).thenReturn(List.of(policy));

        List<LeavePolicy> policies = service.getAllPolicies();

        assertEquals(1, policies.size());
        assertSame(policy, policies.get(0));
    }

    @Test
    void getPolicyByType_ReturnsPolicy() {
        LeavePolicy policy = new LeavePolicy();
        policy.setLeaveType(LeaveType.SICK);
        when(repository.findByLeaveType(LeaveType.SICK)).thenReturn(Optional.of(policy));

        LeavePolicy result = service.getPolicyByType(LeaveType.SICK);

        assertEquals(LeaveType.SICK, result.getLeaveType());
    }

    @Test
    void getPolicyByType_ThrowsWhenMissing() {
        when(repository.findByLeaveType(LeaveType.SICK)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getPolicyByType(LeaveType.SICK));

        assertEquals("Policy not found for type: SICK", exception.getMessage());
    }

    @Nested
    class CreateOrUpdateTests {

        @Test
        void createOrUpdatePolicy_CreatesNewPolicy() {
            LeavePolicyDto dto = policyDto();
            when(repository.findByLeaveType(LeaveType.ANNUAL)).thenReturn(Optional.empty());
            when(repository.save(any(LeavePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LeavePolicy saved = service.createOrUpdatePolicy(dto);

            assertEquals(LeaveType.ANNUAL, saved.getLeaveType());
            assertEquals(new BigDecimal("18"), saved.getDaysAllowed());
            assertEquals(new BigDecimal("5"), saved.getMaxCarryForwardDays());
            assertEquals(3, saved.getMinDaysNotice());
        }

        @Test
        void createOrUpdatePolicy_ReusesExistingPolicyAndKeepsMinDaysNoticeWhenNull() {
            LeavePolicy existing = new LeavePolicy();
            existing.setLeaveType(LeaveType.ANNUAL);
            existing.setMinDaysNotice(7);

            LeavePolicyDto dto = policyDto();
            dto.setMinDaysNotice(null);
            dto.setCarryForwardAllowed(false);
            dto.setRequiresDelegate(false);
            dto.setActive(false);

            when(repository.findByLeaveType(LeaveType.ANNUAL)).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            LeavePolicy saved = service.createOrUpdatePolicy(dto);

            assertSame(existing, saved);
            assertEquals(7, saved.getMinDaysNotice());
            assertFalse(saved.isCarryForwardAllowed());
            assertFalse(saved.isRequiresDelegate());
            assertFalse(saved.isActive());
        }
    }

    @Test
    void deletePolicy_RemovesExistingPolicy() {
        LeavePolicy policy = new LeavePolicy();
        when(repository.findById("POL-1")).thenReturn(Optional.of(policy));

        service.deletePolicy("POL-1");

        verify(repository).delete(policy);
    }

    @Test
    void deletePolicy_ThrowsWhenMissing() {
        when(repository.findById("POL-1")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.deletePolicy("POL-1"));

        assertEquals("Policy not found", exception.getMessage());
    }

    private LeavePolicyDto policyDto() {
        LeavePolicyDto dto = new LeavePolicyDto();
        dto.setLeaveType(LeaveType.ANNUAL);
        dto.setDaysAllowed(new BigDecimal("18"));
        dto.setCarryForwardAllowed(true);
        dto.setMaxCarryForwardDays(new BigDecimal("5"));
        dto.setRequiresDelegate(true);
        dto.setMinDaysNotice(3);
        dto.setActive(true);
        return dto;
    }
}
