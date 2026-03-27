package com.tms.ls.service;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;

import java.util.List;

public interface HolidayService {
    List<HolidayResponse> getAllHolidays();
    HolidayResponse addHoliday(HolidayRequest request);
    void deleteHoliday(String id);
}
