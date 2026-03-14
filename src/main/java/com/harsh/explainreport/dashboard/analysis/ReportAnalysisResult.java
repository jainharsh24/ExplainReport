package com.harsh.explainreport.dashboard.analysis;

import com.harsh.explainreport.dashboard.dto.AnalysisResponse;
import com.harsh.explainreport.dashboard.dto.RedFlagReport;

public class ReportAnalysisResult {

    private final AnalysisResponse analysisResponse;
    private final RedFlagReport redFlagReport;
    private final ReportContext context;

    public ReportAnalysisResult(AnalysisResponse analysisResponse,
                                RedFlagReport redFlagReport,
                                ReportContext context) {
        this.analysisResponse = analysisResponse;
        this.redFlagReport = redFlagReport;
        this.context = context;
    }

    public AnalysisResponse getAnalysisResponse() {
        return analysisResponse;
    }

    public RedFlagReport getRedFlagReport() {
        return redFlagReport;
    }

    public ReportContext getContext() {
        return context;
    }
}
