package com.harsh.explainreport.dashboard.analysis;

public class RangeCheck {

    public enum Status {
        LOW,
        NORMAL,
        HIGH,
        UNKNOWN
    }

    private final String name;
    private final double value;
    private final String unit;
    private final Double min;
    private final Double max;
    private final Status status;
    private final String note;

    public RangeCheck(String name, double value, String unit, Double min, Double max, Status status, String note) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.status = status;
        this.note = note;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Status getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
