package com.harsh.explainreport.dashboard.analysis;

import com.harsh.explainreport.dashboard.dto.RedFlagReport;

import java.util.List;

public class ReportContext {

    private final String reportText;
    private final List<ParameterValue> parameters;
    private final List<RangeCheck> rangeChecks;
    private final List<RangeCheck> abnormalChecks;
    private final List<String> riskFindings;
    private final List<String> summary;
    private final List<String> keyFindings;
    private final List<String> riskFlags;
    private final List<String> doctorQuestions;
    private final List<String> nextSteps;
    private final RedFlagReport redFlagReport;

    public ReportContext(String reportText,
                         List<ParameterValue> parameters,
                         List<RangeCheck> rangeChecks,
                         List<RangeCheck> abnormalChecks,
                         List<String> riskFindings,
                         List<String> summary,
                         List<String> keyFindings,
                         List<String> riskFlags,
                         List<String> doctorQuestions,
                         List<String> nextSteps,
                         RedFlagReport redFlagReport) {
        this.reportText = reportText;
        this.parameters = parameters;
        this.rangeChecks = rangeChecks;
        this.abnormalChecks = abnormalChecks;
        this.riskFindings = riskFindings;
        this.summary = summary;
        this.keyFindings = keyFindings;
        this.riskFlags = riskFlags;
        this.doctorQuestions = doctorQuestions;
        this.nextSteps = nextSteps;
        this.redFlagReport = redFlagReport;
    }

    public String getReportText() {
        return reportText;
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }

    public List<RangeCheck> getRangeChecks() {
        return rangeChecks;
    }

    public List<RangeCheck> getAbnormalChecks() {
        return abnormalChecks;
    }

    public List<String> getRiskFindings() {
        return riskFindings;
    }

    public List<String> getSummary() {
        return summary;
    }

    public List<String> getKeyFindings() {
        return keyFindings;
    }

    public List<String> getRiskFlags() {
        return riskFlags;
    }

    public List<String> getDoctorQuestions() {
        return doctorQuestions;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public RedFlagReport getRedFlagReport() {
        return redFlagReport;
    }
}
