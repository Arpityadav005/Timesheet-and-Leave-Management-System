package com.tms.ls.dto;

import com.tms.ls.entity.HolidayType;
import java.time.LocalDate;

public class HolidayResponse {
    private String id;
    private LocalDate date;
    private String name;
    private HolidayType type;

    public HolidayResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public HolidayType getType() { return type; }
    public void setType(HolidayType type) { this.type = type; }
}
