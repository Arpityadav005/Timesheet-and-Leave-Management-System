package com.tms.ls.controller;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.service.HolidayService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/leave/holidays")
@SecurityRequirement(name = "bearerAuth")
public class HolidayController {
    private static final Logger log = LoggerFactory.getLogger(HolidayController.class);

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> getAllHolidays() {
        log.info("Fetching all holidays");
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponse> addHoliday(@Valid @RequestBody HolidayRequest request) {
        log.info("Creating holiday name={} date={}", request.getName(), request.getDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.addHoliday(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable String id) {
        log.info("Deleting holiday id={}", id);
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
