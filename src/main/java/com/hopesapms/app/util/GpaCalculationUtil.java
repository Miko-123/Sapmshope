package com.hopesapms.app.util;

import java.math.BigDecimal;

public class GpaCalculationUtil {

    /**
     * Converts a percentage (0.00 - 100.00) to a letter grade.
     */
    public static String calculateLetterGrade(BigDecimal percentage) {
        if (percentage == null) return "N/A";
        double p = percentage.doubleValue();
        if (p >= 90) return "A";
        if (p >= 80) return "B";
        if (p >= 70) return "C";
        if (p >= 60) return "D";
        return "F";
    }

    /**
     * Converts a letter grade to a standard 4.0 scale grade point.
     */
    public static Double getGradePoint(String letterGrade) {
        if (letterGrade == null) return 0.0;
        switch (letterGrade) {
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
}