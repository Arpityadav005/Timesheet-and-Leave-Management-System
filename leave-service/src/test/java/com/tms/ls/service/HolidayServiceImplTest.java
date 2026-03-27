package com.tms.ls.service;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.entity.Holiday;
import com.tms.ls.entity.HolidayType;
import com.tms.ls.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayServiceImpl service;

    @Test
    void getAllHolidays_ReturnsMappedResponsesInRepositoryOrder() {
        Holiday holiday = new Holiday(LocalDate.of(2026, 4, 14), "Festival", HolidayType.MANDATORY);
        holiday.setId("HOL-1");
        when(holidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(holiday));

        List<HolidayResponse> responses = service.getAllHolidays();

        assertEquals(1, responses.size());
        assertEquals("HOL-1", responses.get(0).getId());
        assertEquals("Festival", responses.get(0).getName());
        assertEquals(HolidayType.MANDATORY, responses.get(0).getType());
    }

    @Test
    void addHoliday_SavesNewHolidayAndReturnsMappedResponse() {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.of(2026, 12, 25));
        request.setName("Holiday");
        request.setType(HolidayType.OPTIONAL);

        when(holidayRepository.findByDate(request.getDate())).thenReturn(Optional.empty());
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(invocation -> {
            Holiday holiday = invocation.getArgument(0);
            holiday.setId("HOL-1");
            return holiday;
        });

        HolidayResponse response = service.addHoliday(request);

        assertEquals("HOL-1", response.getId());
        assertEquals("Holiday", response.getName());
        assertEquals(HolidayType.OPTIONAL, response.getType());
    }

    @Test
    void addHoliday_ThrowsWhenDateAlreadyExists() {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.of(2026, 12, 25));
        request.setName("Holiday");
        request.setType(HolidayType.OPTIONAL);

        when(holidayRepository.findByDate(request.getDate())).thenReturn(Optional.of(new Holiday()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addHoliday(request));

        assertEquals("A holiday already exists for this date", exception.getMessage());
    }

    @Test
    void deleteHoliday_DelegatesToRepository() {
        service.deleteHoliday("HOL-1");

        verify(holidayRepository).deleteById("HOL-1");
    }
}
