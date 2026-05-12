package com.hopesapms.app.common.util;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PeriodUtil {

    public static List<Integer> parsePeriods(String rawInput) {
        List<Integer> periods = new ArrayList<>();
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return periods;
        }

        String clean = rawInput.replaceAll("\\s+", "");

        if (clean.contains("-")) {
            try {
                String[] parts = clean.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) {
                    periods.add(i);
                }
            } catch (NumberFormatException e) {

            }
        }

        else {
            String[] parts = clean.split(",");
            for (String part : parts) {
                try {
                    periods.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {

                }
            }
        }
        return periods;
    }

    public static int calculateDuration(String rawInput) {
        return parsePeriods(rawInput).size();
    }

    public static String normalize(String rawInput) {
        List<Integer> list = parsePeriods(rawInput);
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
