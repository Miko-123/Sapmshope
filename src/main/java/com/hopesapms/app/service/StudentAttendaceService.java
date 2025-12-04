package com.hopesapms.app.service;

import com.hopesapms.app.model.StudentAttendance;
import com.hopesapms.app.repository.StudentAttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAttendaceService {
    @Autowired
    private StudentAttendanceRepository studentAttendaceRepository;

    @Transactional
    public StudentAttendance recordAttendance(StudentAttendance attendance) {
        if (attendance.getStatus() == null || attendance.getStatus().isEmpty()) {
            throw new IllegalArgumentException("Attendance status is required");
        }
        return studentAttendaceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public Page<Object []> getAttendancePercentageByStudent(Pageable pageable) {
        return studentAttendaceRepository.findAttendancePercentageByStudent(pageable);
    }

    @Transactional(readOnly = true)
    public Page<StudentAttendance> findByClassSessionId(Integer classSessionId, Pageable pageable) {
        return studentAttendaceRepository.findByClassSessionId(classSessionId, pageable);
    }
}
