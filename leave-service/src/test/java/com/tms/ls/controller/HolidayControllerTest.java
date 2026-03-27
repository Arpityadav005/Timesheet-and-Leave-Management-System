package com.tms.ls.controller;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.entity.HolidayType;
import com.tms.ls.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayControllerTest {

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private HolidayController controller;

    @Test
    void getAllHolidays_ReturnsOkResponse() {
        HolidayResponse holiday = new HolidayResponse();
        holiday.setId("HOL-1");
        when(holidayService.getAllHolidays()).thenReturn(List.of(holiday));

        ResponseEntity<List<HolidayResponse>> response = controller.getAllHolidays();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void addHoliday_ReturnsCreatedResponse() {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.now().plusDays(5));
        request.setName("Festival");
        request.setType(HolidayType.MANDATORY);

        HolidayResponse holiday = new HolidayResponse();
        holiday.setId("HOL-1");

        when(holidayService.addHoliday(request)).thenReturn(holiday);

        ResponseEntity<HolidayResponse> response = controller.addHoliday(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("HOL-1", response.getBody().getId());
    }

    @Test
    void deleteHoliday_ReturnsNoContentAndDelegates() {
        ResponseEntity<Void> response = controller.deleteHoliday("HOL-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(holidayService).deleteHoliday("HOL-1");
    }
}
