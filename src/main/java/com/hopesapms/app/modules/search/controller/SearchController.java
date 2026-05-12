package com.hopesapms.app.modules.search.controller;

import com.hopesapms.app.modules.search.dto.GlobalSearchResponseDTO;
import com.hopesapms.app.modules.search.dto.GlobalSearchResponseDTO.SearchResultItem;
import com.hopesapms.app.modules.course.model.Course;
import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.course.repository.CourseRepository;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final UserRepository userRepository; 

    @GetMapping("/global")
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalSearchResponseDTO> globalSearch(
            @RequestParam String query,
            Authentication authentication) { 
        
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(GlobalSearchResponseDTO.builder()
                .students(List.of()).courses(List.of()).instructors(List.of()).build());
        }

        User currentUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        
        boolean isDeptHead = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("DEPARTMENT_HEAD"));
        
        Long deptId = (isDeptHead && currentUser.getDepartment() != null) 
                ? currentUser.getDepartment().getId() 
                : null;

        List<SearchResultItem> students;
        if (deptId != null) {
            students = studentRepository.searchStudentsByDepartment(query, deptId, PageRequest.of(0, 5))
                .stream().map(this::mapStudent).collect(Collectors.toList());
        } else {
            students = studentRepository.searchStudents(query, PageRequest.of(0, 5))
                .stream().map(this::mapStudent).collect(Collectors.toList());
        }

        List<SearchResultItem> courses;
        if (deptId != null) {
             
             courses = courseRepository.searchCoursesByDepartment(query, deptId)
                 .stream().limit(5).map(this::mapCourse).collect(Collectors.toList());
        } else {
           
             courses = courseRepository.searchCourses(query)
                 .stream().limit(5).map(this::mapCourse).collect(Collectors.toList());
        }

        List<SearchResultItem> instructors = instructorRepository.searchInstructors(query)
            .stream().limit(5)
            .map(this::mapInstructor)
            .collect(Collectors.toList());

        return ResponseEntity.ok(GlobalSearchResponseDTO.builder()
            .students(students)
            .courses(courses)
            .instructors(instructors)
            .build());
    }

    private SearchResultItem mapStudent(Student s) {
        User u = s.getUser();

        String fullName = u.getFirstName() + " " + u.getMiddleName() + " " + u.getLastName();

        return SearchResultItem.builder()
            .id(s.getId().longValue())
            .title(fullName)
            .subtitle(s.getStudentId())
            .type("STUDENT")
            .url("/registrar/students?search=" + s.getStudentId())
            .build();
    }

    private SearchResultItem mapCourse(Course c) {
        return SearchResultItem.builder()
            .id(c.getId().longValue())
            .title(c.getTitle())
            .subtitle(c.getCourseCode())
            .type("COURSE")
            .url("/department-head/course-management?search=" + c.getCourseCode())
            .build();
    }

    private SearchResultItem mapInstructor(Instructor i) {
        return SearchResultItem.builder()
            .id(i.getId())
            .title(i.getUser().getFirstName() + " " + i.getUser().getLastName())
            .subtitle(i.getUser().getEmail())
            .type("INSTRUCTOR")
            .url("/department-head/faculty")
            .build();
    }
}