package com.harsh.explainreport.dashboard.analysis;

public class ParameterValue {

    private final String name;
    private final double value;
    private final String unit;
    private final String sourceLine;

    public ParameterValue(String name, double value, String unit, String sourceLine) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.sourceLine = sourceLine;
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

    public String getSourceLine() {
        return sourceLine;
    }
}
