package com.harsh.explainreport.dashboard.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TextParsing {

    private TextParsing() {
    }

    public static List<String> parseList(String section) {
        if (section == null || section.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(section.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.replaceFirst("^(?:[-*]|\\d+[).])\\s*", ""))
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    public static String limitWords(String text, int maxWords) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String[] words = trimmed.split("\\s+");
        if (words.length <= maxWords) {
            return trimmed;
        }

        return String.join(" ", Arrays.copyOfRange(words, 0, maxWords)) + "...";
    }
}
