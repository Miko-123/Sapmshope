package com.hopesapms.app.service;

import com.hopesapms.app.model.GradingScale;
import com.hopesapms.app.repository.GradingScaleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingService {

    private final GradingScaleRepository gradingScaleRepository;

    @PostConstruct
    public void initDefaultScale() {
        if (gradingScaleRepository.count() == 0) {
            List<GradingScale> defaults = List.of(
                    GradingScale.builder().letterGrade("A+").minScore(90.0).maxScore(100.0).gradePoint(4.0)
                            .description("Excellent").build(),
                    GradingScale.builder().letterGrade("A").minScore(85.0).maxScore(89.99).gradePoint(4.0)
                            .description("Excellent").build(),
                    GradingScale.builder().letterGrade("A-").minScore(80.0).maxScore(84.99).gradePoint(3.75)
                            .description("Excellent").build(),
                    GradingScale.builder().letterGrade("B+").minScore(75.0).maxScore(79.99).gradePoint(3.5)
                            .description("Very Good").build(),
                    GradingScale.builder().letterGrade("B").minScore(70.0).maxScore(74.99).gradePoint(3.0)
                            .description("Good").build(),
                    GradingScale.builder().letterGrade("B-").minScore(65.0).maxScore(69.99).gradePoint(2.75)
                            .description("Good").build(),
                    GradingScale.builder().letterGrade("C+").minScore(60.0).maxScore(64.99).gradePoint(2.5)
                            .description("Satisfactory").build(),
                    GradingScale.builder().letterGrade("C").minScore(50.0).maxScore(59.99).gradePoint(2.0)
                            .description("Satisfactory").build(),
                    GradingScale.builder().letterGrade("D").minScore(40.0).maxScore(49.99).gradePoint(1.0)
                            .description("Poor").build(),
                    GradingScale.builder().letterGrade("F").minScore(0.0).maxScore(39.99).gradePoint(0.0)
                            .description("Failure").build());
            gradingScaleRepository.saveAll(defaults);
        }
    }

    @Transactional(readOnly = true)
    public GradingScale calculateGrade(Double totalScore) {
        if (totalScore == null)
            return null;

        if (totalScore > 100.0)
            totalScore = 100.0;

        if (totalScore < 0)
            totalScore = 0.0;

        return gradingScaleRepository.findByScore(totalScore)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Double getPointsForLetter(String letterGrade) {
        return gradingScaleRepository.findAll().stream()
                .filter(g -> g.getLetterGrade().equalsIgnoreCase(letterGrade))
                .findFirst()
                .map(GradingScale::getGradePoint)
                .orElse(0.0);
    }
}