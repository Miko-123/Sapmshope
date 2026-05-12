package com.hopesapms.app.modules.schedule.repository;

import com.hopesapms.app.modules.schedule.model.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    List<CourseSchedule> findByDay(String day);

    List<CourseSchedule> findByCourseOffering_Id(Long courseOfferingId);
}