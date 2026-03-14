package com.harsh.explainreport.dashboard.analysis;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RangeChecker {

    private static final Map<String, NormalRange> NORMAL_RANGES = buildRanges();

    public List<RangeCheck> checkNormalRanges(List<ParameterValue> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }

        List<RangeCheck> results = new ArrayList<>();
        for (ParameterValue parameter : parameters) {
            NormalRange range = NORMAL_RANGES.get(parameter.getName());
            if (range == null) {
                results.add(new RangeCheck(
                        parameter.getName(),
                        parameter.getValue(),
                        parameter.getUnit(),
                        null,
                        null,
                        RangeCheck.Status.UNKNOWN,
                        "No reference range available."
                ));
                continue;
            }

            boolean compatible = unitsCompatible(parameter.getUnit(), range.getUnit());
            if (!compatible) {
                results.add(new RangeCheck(
                        parameter.getName(),
                        parameter.getValue(),
                        parameter.getUnit(),
                        range.getMin(),
                        range.getMax(),
                        RangeCheck.Status.UNKNOWN,
                        "Units do not match reference range."
                ));
                continue;
            }

            RangeCheck.Status status = RangeCheck.Status.NORMAL;
            if (range.getMin() != null && parameter.getValue() < range.getMin()) {
                status = RangeCheck.Status.LOW;
            } else if (range.getMax() != null && parameter.getValue() > range.getMax()) {
                status = RangeCheck.Status.HIGH;
            }

            results.add(new RangeCheck(
                    parameter.getName(),
                    parameter.getValue(),
                    normalizeUnit(parameter.getUnit()),
                    range.getMin(),
                    range.getMax(),
                    status,
                    range.getNote()
            ));
        }

        return results;
    }

    private static boolean unitsCompatible(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null || actual.isBlank()) {
            return true;
        }
        return normalizeUnit(actual).equalsIgnoreCase(normalizeUnit(expected));
    }

    private static String normalizeUnit(String unit) {
        return unit == null ? "" : unit.replaceAll("\\s+", "");
    }

    private static Map<String, NormalRange> buildRanges() {
        Map<String, NormalRange> ranges = new LinkedHashMap<>();
        ranges.put("Hemoglobin", new NormalRange("Hemoglobin", 12.0, 17.5, "g/dL", "Typical adult range."));
        ranges.put("WBC", new NormalRange("WBC", 4000.0, 11000.0, "/uL", "Typical adult range."));
        ranges.put("RBC", new NormalRange("RBC", 4.0, 5.9, "million/uL", "Typical adult range."));
        ranges.put("Platelets", new NormalRange("Platelets", 150000.0, 450000.0, "/uL", "Typical adult range."));
        ranges.put("Hematocrit", new NormalRange("Hematocrit", 36.0, 52.0, "%", "Typical adult range."));
        ranges.put("MCV", new NormalRange("MCV", 80.0, 100.0, "fL", "Typical adult range."));
        ranges.put("MCH", new NormalRange("MCH", 27.0, 33.0, "pg", "Typical adult range."));
        ranges.put("MCHC", new NormalRange("MCHC", 32.0, 36.0, "g/dL", "Typical adult range."));
        ranges.put("RDW", new NormalRange("RDW", 11.5, 14.5, "%", "Typical adult range."));
        ranges.put("Glucose (Fasting)", new NormalRange("Glucose (Fasting)", 70.0, 99.0, "mg/dL", "Typical fasting range."));
        ranges.put("Glucose (Random)", new NormalRange("Glucose (Random)", 70.0, 140.0, "mg/dL", "Typical random range."));
        ranges.put("Glucose", new NormalRange("Glucose", 70.0, 140.0, "mg/dL", "General range if fasting is unknown."));
        ranges.put("HbA1c", new NormalRange("HbA1c", 4.0, 5.6, "%", "Typical non-diabetic range."));
        ranges.put("Creatinine", new NormalRange("Creatinine", 0.6, 1.3, "mg/dL", "Typical adult range."));
        ranges.put("BUN", new NormalRange("BUN", 7.0, 20.0, "mg/dL", "Typical adult range."));
        ranges.put("eGFR", new NormalRange("eGFR", 60.0, null, "mL/min", "Values above 60 are generally normal."));
        ranges.put("Sodium", new NormalRange("Sodium", 135.0, 145.0, "mEq/L", "Typical adult range."));
        ranges.put("Potassium", new NormalRange("Potassium", 3.5, 5.1, "mEq/L", "Typical adult range."));
        ranges.put("Chloride", new NormalRange("Chloride", 98.0, 107.0, "mEq/L", "Typical adult range."));
        ranges.put("Calcium", new NormalRange("Calcium", 8.5, 10.5, "mg/dL", "Typical adult range."));
        ranges.put("ALT", new NormalRange("ALT", 7.0, 56.0, "U/L", "Typical adult range."));
        ranges.put("AST", new NormalRange("AST", 10.0, 40.0, "U/L", "Typical adult range."));
        ranges.put("Alkaline Phosphatase", new NormalRange("Alkaline Phosphatase", 44.0, 147.0, "U/L", "Typical adult range."));
        ranges.put("Total Bilirubin", new NormalRange("Total Bilirubin", 0.1, 1.2, "mg/dL", "Typical adult range."));
        ranges.put("Albumin", new NormalRange("Albumin", 3.5, 5.0, "g/dL", "Typical adult range."));
        ranges.put("Total Cholesterol", new NormalRange("Total Cholesterol", 0.0, 200.0, "mg/dL", "Desirable level."));
        ranges.put("HDL", new NormalRange("HDL", 40.0, null, "mg/dL", "Higher is better."));
        ranges.put("LDL", new NormalRange("LDL", 0.0, 100.0, "mg/dL", "Optimal level."));
        ranges.put("Triglycerides", new NormalRange("Triglycerides", 0.0, 150.0, "mg/dL", "Normal fasting level."));
        ranges.put("CRP", new NormalRange("CRP", 0.0, 10.0, "mg/L", "Lower values are better."));
        ranges.put("TSH", new NormalRange("TSH", 0.4, 4.0, "uIU/mL", "Typical adult range."));
        ranges.put("Vitamin D", new NormalRange("Vitamin D", 20.0, 50.0, "ng/mL", "General sufficiency range."));
        return ranges;
    }

}
