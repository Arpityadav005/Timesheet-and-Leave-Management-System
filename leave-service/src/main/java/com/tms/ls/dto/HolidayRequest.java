package com.tms.ls.dto;

import com.tms.ls.entity.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class HolidayRequest {
    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Holiday name is required")
    private String name;

    @NotNull(message = "Holiday type is required")
    private HolidayType type;

    public HolidayRequest() {}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public HolidayType getType() { return type; }
    public void setType(HolidayType type) { this.type = type; }
}
