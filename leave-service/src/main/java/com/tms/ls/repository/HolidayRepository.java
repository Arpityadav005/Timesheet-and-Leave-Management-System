package com.tms.ls.repository;

import com.tms.ls.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, String> {
    Optional<Holiday> findByDate(LocalDate date);
    List<Holiday> findAllByOrderByDateAsc();
}
