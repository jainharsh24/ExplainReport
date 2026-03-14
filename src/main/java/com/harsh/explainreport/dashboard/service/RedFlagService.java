package com.harsh.explainreport.dashboard.service;

import com.harsh.explainreport.dashboard.dto.RedFlagReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RedFlagService {
    private static final int MIN_CRITICAL_COUNT = 3;

    private static final List<Rule> RULES = List.of(
            new Rule("Hemoglobin", patternFor("hemoglobin|hb\\b"), RuleType.LOW, 8.0, Category.BLOOD_COUNTS),
            new Rule("Hemoglobin", patternFor("hemoglobin|hb\\b"), RuleType.HIGH, 18.0, Category.BLOOD_COUNTS),
            new Rule("WBC", patternFor("wbc|white\\s*blood\\s*cell"), RuleType.HIGH, 20000, Category.BLOOD_COUNTS),
            new Rule("WBC", patternFor("wbc|white\\s*blood\\s*cell"), RuleType.LOW, 2500, Category.BLOOD_COUNTS),
            new Rule("Platelets", patternFor("platelets|plt"), RuleType.LOW, 50000, Category.BLOOD_COUNTS),
            new Rule("Hematocrit", patternFor("hematocrit|hct"), RuleType.LOW, 25, Category.BLOOD_COUNTS),
            new Rule("MCV", patternFor("mcv|mean\\s*corpuscular\\s*volume"), RuleType.HIGH, 120, Category.BLOOD_COUNTS),
            new Rule("MCV", patternFor("mcv|mean\\s*corpuscular\\s*volume"), RuleType.LOW, 60, Category.BLOOD_COUNTS),
            new Rule("Creatinine", patternFor("creatinine"), RuleType.HIGH, 4.0, Category.KIDNEY),
            new Rule("BUN", patternFor("bun|blood\\s*urea\\s*nitrogen|urea"), RuleType.HIGH, 80, Category.KIDNEY),
            new Rule("Potassium", patternFor("potassium|k\\+"), RuleType.HIGH, 6.0, Category.ELECTROLYTES),
            new Rule("eGFR", patternFor("egfr|e-gfr"), RuleType.LOW, 15, Category.KIDNEY),
            new Rule("Sodium", patternFor("sodium|na\\+"), RuleType.LOW, 120, Category.ELECTROLYTES),
            new Rule("Sodium", patternFor("sodium|na\\+"), RuleType.HIGH, 160, Category.ELECTROLYTES),
            new Rule("Total Bilirubin", patternFor("total\\s*bilirubin|bilirubin"), RuleType.HIGH, 10, Category.LIVER),
            new Rule("ALT", patternFor("alt|sgpt|alanine\\s*aminotransferase"), RuleType.HIGH, 500, Category.LIVER),
            new Rule("AST", patternFor("ast|sgot|aspartate\\s*aminotransferase"), RuleType.HIGH, 500, Category.LIVER),
            new Rule("Alkaline Phosphatase", patternFor("alkaline\\s*phosphatase|alp"), RuleType.HIGH, 350, Category.LIVER),
            new Rule("Albumin", patternFor("albumin"), RuleType.LOW, 2.5, Category.LIVER),
            new Rule("Troponin", patternFor("troponin"), RuleType.HIGH, 1.0, Category.CARDIAC),
            new Rule("CK-MB", patternFor("ck\\s*-?mb"), RuleType.HIGH, 80, Category.CARDIAC),
            new Rule("Heart Rate", patternFor("heart\\s*rate|pulse"), RuleType.HIGH, 130, Category.CARDIAC),
            new Rule("Heart Rate", patternFor("heart\\s*rate|pulse"), RuleType.LOW, 40, Category.CARDIAC),
            new Rule("Fasting Blood Sugar", patternFor("fasting\\s*blood\\s*sugar|fasting\\s*glucose"), RuleType.HIGH, 300, Category.GLUCOSE),
            new Rule("Random Blood Sugar", patternFor("random\\s*blood\\s*sugar|random\\s*glucose"), RuleType.HIGH, 400, Category.GLUCOSE),
            new Rule("HbA1c", patternFor("hba1c|hb\\s*a1c|glycated\\s*hemoglobin"), RuleType.HIGH, 10, Category.GLUCOSE),
            new Rule("CRP", patternFor("c-?reactive\\s*protein|crp"), RuleType.HIGH, 100, Category.INFECTION),
            new Rule("Procalcitonin", patternFor("procalcitonin|pct"), RuleType.HIGH, 5, Category.INFECTION),
            new Rule("Lactate", patternFor("lactate"), RuleType.HIGH, 4, Category.INFECTION)
    );

    private static final List<KeywordRule> KEYWORD_RULES = List.of(
            new KeywordRule(patternFor("acute\\s*liver\\s*failure"), "Acute liver failure mentioned.", Category.LIVER),
            new KeywordRule(patternFor("hepatic\\s*encephalopathy"), "Hepatic encephalopathy risk mentioned.", Category.LIVER),
            new KeywordRule(patternFor("st-?segment\\s*elevation"), "ST-segment elevation mentioned.", Category.CARDIAC),
            new KeywordRule(patternFor("myocardial\\s*infarction"), "Acute myocardial infarction mentioned.", Category.CARDIAC),
            new KeywordRule(patternFor("urgent\\s*icu|icu\\s*admission"), "ICU admission recommended.", Category.INFECTION)
    );

    private static final Pattern BP_PATTERN = Pattern.compile("(?i)(bp|blood\\s*pressure)\\s*[:\\-]?\\s*(\\d{2,3})\\s*/\\s*(\\d{2,3})");

    public RedFlagReport evaluate(String reportText) {
        if (reportText == null || reportText.isBlank()) {
            return new RedFlagReport(false, List.of(), List.of(), null);
        }

        List<String> findings = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        EnumSet<Category> categories = EnumSet.noneOf(Category.class);

        String[] lines = reportText.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            for (KeywordRule keywordRule : KEYWORD_RULES) {
                if (keywordRule.pattern.matcher(line).find()) {
                    if (matched.add(keywordRule.message)) {
                        findings.add(keywordRule.message);
                        categories.add(keywordRule.category);
                    }
                }
            }

            Matcher bpMatcher = BP_PATTERN.matcher(line);
            if (bpMatcher.find()) {
                int systolic = parseInt(bpMatcher.group(2));
                int diastolic = parseInt(bpMatcher.group(3));
                if (systolic >= 180 || diastolic >= 120) {
                    if (matched.add("Blood Pressure")) {
                        findings.add("Critical blood pressure " + systolic + "/" + diastolic + " (>= 180/120).");
                        categories.add(Category.CARDIAC);
                    }
                }
                if (systolic > 0 && diastolic > 0 && (systolic <= 80 || diastolic <= 50)) {
                    if (matched.add("Blood Pressure Low")) {
                        findings.add("Critical blood pressure " + systolic + "/" + diastolic + " (<= 80/50).");
                        categories.add(Category.CARDIAC);
                    }
                }
            }

            for (Rule rule : RULES) {
                Matcher matcher = rule.pattern.matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                Double value = extractValue(line, matcher.end());
                if (value == null) {
                    continue;
                }
                if (rule.isCritical(value)) {
                    String key = rule.name + ":" + rule.type;
                    if (matched.add(key)) {
                        findings.add(rule.toMessage(value));
                        categories.add(rule.category);
                    }
                }
            }
        }

        boolean active = findings.size() >= MIN_CRITICAL_COUNT;
        List<String> instructions = active ? criticalInstructions(categories) : List.of();
        String summary = active ? buildSummary(categories) : null;

        return new RedFlagReport(active, findings, instructions, summary);
    }

    private static Pattern patternFor(String keyword) {
        return Pattern.compile("(?i)\\b(" + keyword + ")\\b");
    }

    private static Double extractValue(String line, int startIndex) {
        String tail = line.substring(Math.min(startIndex, line.length()));
        Matcher matcher = Pattern.compile("(-?\\d{1,4}(?:,\\d{3})*(?:\\.\\d+)?)").matcher(tail);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static List<String> criticalInstructions(Set<Category> categories) {
        List<String> instructions = new ArrayList<>();
        instructions.add("Seek urgent in-person medical evaluation as soon as possible.");
        instructions.add("Bring this report and a list of current medicines and supplements.");

        if (categories.contains(Category.CARDIAC)) {
            instructions.add("If you have chest pain, shortness of breath, fainting, or severe palpitations, go to emergency care immediately.");
            instructions.add("Ask about urgent ECG and repeat cardiac marker testing.");
        }
        if (categories.contains(Category.KIDNEY)) {
            instructions.add("Ask about repeat kidney function tests and urine analysis.");
            instructions.add("Avoid NSAIDs or contrast exposure unless a clinician advises otherwise.");
        }
        if (categories.contains(Category.LIVER)) {
            instructions.add("Avoid alcohol and acetaminophen until reviewed.");
            instructions.add("Ask about repeat liver panel and imaging if advised.");
        }
        if (categories.contains(Category.ELECTROLYTES)) {
            instructions.add("Do not take electrolyte supplements or diuretics unless advised.");
            instructions.add("Ask about urgent electrolyte recheck and ECG monitoring.");
        }
        if (categories.contains(Category.BLOOD_COUNTS)) {
            instructions.add("Report any bleeding, easy bruising, or fever promptly.");
            instructions.add("Ask about repeat CBC and possible urgent interventions.");
        }
        if (categories.contains(Category.GLUCOSE)) {
            instructions.add("If you have vomiting, confusion, or rapid breathing, seek emergency care.");
            instructions.add("Ask about urgent glucose management and ketone testing.");
        }
        if (categories.contains(Category.INFECTION)) {
            instructions.add("Ask about infection source evaluation, cultures, and whether urgent antibiotics are needed.");
        }

        if (instructions.size() == 2) {
            instructions.add("If severe symptoms occur, go to emergency care immediately.");
        }
        return instructions;
    }

    private static String buildSummary(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return "Critical alert triggered by multiple high-risk indicators. Review the critical findings below.";
        }
        List<String> labels = new ArrayList<>();
        for (Category category : categories) {
            labels.add(category.label);
        }
        return "Critical alert triggered by high-risk indicators involving " + formatList(labels) + ". Review the critical findings below.";
    }

    private static String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "clinical markers";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        if (items.size() == 2) {
            return items.get(0) + " and " + items.get(1);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(i == items.size() - 1 ? ", and " : ", ");
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    private enum RuleType {
        HIGH, LOW
    }

    private enum Category {
        BLOOD_COUNTS("blood counts"),
        KIDNEY("kidney function"),
        ELECTROLYTES("electrolyte balance"),
        LIVER("liver function"),
        CARDIAC("cardiac markers"),
        GLUCOSE("glucose control"),
        INFECTION("infection or inflammation");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private static class Rule {
        private final String name;
        private final Pattern pattern;
        private final RuleType type;
        private final double threshold;
        private final Category category;

        private Rule(String name, Pattern pattern, RuleType type, double threshold, Category category) {
            this.name = name;
            this.pattern = pattern;
            this.type = type;
            this.threshold = threshold;
            this.category = category;
        }

        private boolean isCritical(double value) {
            return type == RuleType.HIGH ? value >= threshold : value <= threshold;
        }

        private String toMessage(double value) {
            String comparator = type == RuleType.HIGH ? ">=" : "<=";
            return name + " " + value + " (" + comparator + " " + threshold + ").";
        }
    }

    private static class KeywordRule {
        private final Pattern pattern;
        private final String message;
        private final Category category;

        private KeywordRule(Pattern pattern, String message, Category category) {
            this.pattern = pattern;
            this.message = message;
            this.category = category;
        }
    }
}
