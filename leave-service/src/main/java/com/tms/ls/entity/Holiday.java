package com.tms.ls.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "holidays", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"holiday_date"})
})
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private HolidayType type;

    @Column(name = "holiday_year", nullable = false)
    private Integer year;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Holiday() {}

    public Holiday(LocalDate date, String name, HolidayType type) {
        this.date = date;
        this.name = name;
        this.type = type;
        this.year = date.getYear();
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public HolidayType getType() { return type; }
    public void setType(HolidayType type) { this.type = type; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
