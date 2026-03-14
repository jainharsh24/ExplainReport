package com.harsh.explainreport.dashboard.analysis;

public class NormalRange {

    private final String name;
    private final Double min;
    private final Double max;
    private final String unit;
    private final String note;

    public NormalRange(String name, Double min, Double max, String unit, String note) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.note = note;
    }

    public String getName() {
        return name;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public String getUnit() {
        return unit;
    }

    public String getNote() {
        return note;
    }
}
