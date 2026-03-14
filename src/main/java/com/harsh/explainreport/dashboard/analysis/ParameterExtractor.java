package com.harsh.explainreport.dashboard.analysis;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParameterExtractor {

    private static final Pattern VALUE_PATTERN = Pattern.compile("(-?\\d{1,4}(?:,\\d{3})*(?:\\.\\d+)?)");
    private static final Pattern UNIT_PATTERN = Pattern.compile("(?i)(g\\s*/\\s*dl|mg\\s*/\\s*dl|mmol\\s*/\\s*l|meq\\s*/\\s*l|iu\\s*/\\s*l|u\\s*/\\s*l|%|/\\s*u\\s*l|10\\^?3/\\s*u\\s*l|10\\^?9/\\s*l)");

    private static final List<ParameterPattern> PARAMETER_PATTERNS = List.of(
            new ParameterPattern("Hemoglobin", patternFor("hemoglobin|hb\\b"), "g/dL"),
            new ParameterPattern("WBC", patternFor("wbc|white\\s*blood\\s*cell"), "/uL"),
            new ParameterPattern("RBC", patternFor("rbc|red\\s*blood\\s*cell"), "million/uL"),
            new ParameterPattern("Platelets", patternFor("platelets|plt"), "/uL"),
            new ParameterPattern("Hematocrit", patternFor("hematocrit|hct"), "%"),
            new ParameterPattern("MCV", patternFor("mcv|mean\\s*corpuscular\\s*volume"), "fL"),
            new ParameterPattern("MCH", patternFor("mch"), "pg"),
            new ParameterPattern("MCHC", patternFor("mchc"), "g/dL"),
            new ParameterPattern("RDW", patternFor("rdw"), "%"),
            new ParameterPattern("Glucose (Fasting)", patternFor("fasting\\s*glucose|fasting\\s*blood\\s*sugar"), "mg/dL"),
            new ParameterPattern("Glucose (Random)", patternFor("random\\s*glucose|random\\s*blood\\s*sugar"), "mg/dL"),
            new ParameterPattern("Glucose", patternFor("glucose"), "mg/dL"),
            new ParameterPattern("HbA1c", patternFor("hba1c|hb\\s*a1c|glycated\\s*hemoglobin"), "%"),
            new ParameterPattern("Creatinine", patternFor("creatinine"), "mg/dL"),
            new ParameterPattern("BUN", patternFor("bun|blood\\s*urea\\s*nitrogen|urea"), "mg/dL"),
            new ParameterPattern("eGFR", patternFor("egfr|e-gfr"), "mL/min"),
            new ParameterPattern("Sodium", patternFor("sodium|na\\+"), "mEq/L"),
            new ParameterPattern("Potassium", patternFor("potassium|k\\+"), "mEq/L"),
            new ParameterPattern("Chloride", patternFor("chloride|cl\\-"), "mEq/L"),
            new ParameterPattern("Calcium", patternFor("calcium|ca\\+\\+"), "mg/dL"),
            new ParameterPattern("ALT", patternFor("alt|sgpt|alanine\\s*aminotransferase"), "U/L"),
            new ParameterPattern("AST", patternFor("ast|sgot|aspartate\\s*aminotransferase"), "U/L"),
            new ParameterPattern("Alkaline Phosphatase", patternFor("alkaline\\s*phosphatase|alp"), "U/L"),
            new ParameterPattern("Total Bilirubin", patternFor("total\\s*bilirubin|bilirubin"), "mg/dL"),
            new ParameterPattern("Albumin", patternFor("albumin"), "g/dL"),
            new ParameterPattern("Total Cholesterol", patternFor("total\\s*cholesterol"), "mg/dL"),
            new ParameterPattern("HDL", patternFor("hdl"), "mg/dL"),
            new ParameterPattern("LDL", patternFor("ldl"), "mg/dL"),
            new ParameterPattern("Triglycerides", patternFor("triglycerides|tg"), "mg/dL"),
            new ParameterPattern("CRP", patternFor("c-?reactive\\s*protein|crp"), "mg/L"),
            new ParameterPattern("TSH", patternFor("tsh|thyroid\\s*stimulating\\s*hormone"), "uIU/mL"),
            new ParameterPattern("Vitamin D", patternFor("vitamin\\s*d|25-?hydroxy"), "ng/mL")
    );

    public List<ParameterValue> extractParameters(String reportText) {
        if (reportText == null || reportText.isBlank()) {
            return List.of();
        }

        Map<String, ParameterValue> results = new LinkedHashMap<>();
        String[] lines = reportText.split("\\R");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            for (ParameterPattern pattern : PARAMETER_PATTERNS) {
                if (results.containsKey(pattern.name)) {
                    continue;
                }

                Matcher matcher = pattern.pattern.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                Double value = extractValue(line, matcher.end());
                if (value == null) {
                    continue;
                }

                String unit = extractUnit(line, pattern.defaultUnit);
                results.put(pattern.name, new ParameterValue(pattern.name, value, unit, line));
            }
        }

        return new ArrayList<>(results.values());
    }

    private static Pattern patternFor(String keyword) {
        return Pattern.compile("(?i)\\b(" + keyword + ")\\b");
    }

    private static Double extractValue(String line, int startIndex) {
        String tail = line.substring(Math.min(startIndex, line.length()));
        Matcher matcher = VALUE_PATTERN.matcher(tail);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String extractUnit(String line, String fallback) {
        Matcher matcher = UNIT_PATTERN.matcher(line);
        if (matcher.find()) {
            return normalizeUnit(matcher.group(1));
        }
        return fallback == null ? "" : fallback;
    }

    private static String normalizeUnit(String rawUnit) {
        String unit = rawUnit.replaceAll("\\s+", "").toLowerCase();
        if (unit.equals("g/dl")) {
            return "g/dL";
        }
        if (unit.equals("mg/dl")) {
            return "mg/dL";
        }
        if (unit.equals("mmol/l")) {
            return "mmol/L";
        }
        if (unit.equals("meq/l")) {
            return "mEq/L";
        }
        if (unit.equals("iu/l")) {
            return "IU/L";
        }
        if (unit.equals("u/l")) {
            return "U/L";
        }
        if (unit.contains("/ul")) {
            return "/uL";
        }
        return rawUnit.trim();
    }

    private static class ParameterPattern {
        private final String name;
        private final Pattern pattern;
        private final String defaultUnit;

        private ParameterPattern(String name, Pattern pattern, String defaultUnit) {
            this.name = name;
            this.pattern = pattern;
            this.defaultUnit = defaultUnit;
        }
    }
}
