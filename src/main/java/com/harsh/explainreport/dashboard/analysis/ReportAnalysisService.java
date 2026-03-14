package com.harsh.explainreport.dashboard.analysis;

import com.harsh.explainreport.dashboard.dto.AnalysisResponse;
import com.harsh.explainreport.dashboard.dto.GuidedQuery;
import com.harsh.explainreport.dashboard.dto.RedFlagReport;
import com.harsh.explainreport.dashboard.service.GroqService;
import com.harsh.explainreport.dashboard.service.PdfService;
import com.harsh.explainreport.dashboard.service.RedFlagService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportAnalysisService {

    private final PdfService pdfService;
    private final ParameterExtractor parameterExtractor;
    private final RangeChecker rangeChecker;
    private final RiskDetector riskDetector;
    private final SummaryGenerator summaryGenerator;
    private final RedFlagService redFlagService;
    private final GroqService groqService;

    public ReportAnalysisService(PdfService pdfService,
                                 ParameterExtractor parameterExtractor,
                                 RangeChecker rangeChecker,
                                 RiskDetector riskDetector,
                                 SummaryGenerator summaryGenerator,
                                 RedFlagService redFlagService,
                                 GroqService groqService) {
        this.pdfService = pdfService;
        this.parameterExtractor = parameterExtractor;
        this.rangeChecker = rangeChecker;
        this.riskDetector = riskDetector;
        this.summaryGenerator = summaryGenerator;
        this.redFlagService = redFlagService;
        this.groqService = groqService;
    }

    public ReportAnalysisResult analyzeReport(MultipartFile file) {
        String reportText = pdfService.extractText(file);
        List<ParameterValue> parameters = parameterExtractor.extractParameters(reportText);
        List<RangeCheck> rangeChecks = rangeChecker.checkNormalRanges(parameters);
        RedFlagReport redFlagReport = redFlagService.evaluate(reportText);
        List<String> riskFindings = riskDetector.detectHealthRisk(reportText, rangeChecks, redFlagReport);
        AnalysisResponse analysisResponse = summaryGenerator.generateSummary(
                reportText,
                rangeChecks,
                riskFindings,
                redFlagReport
        );

        ReportContext context = new ReportContext(
                reportText,
                parameters,
                rangeChecks,
                abnormalChecks(rangeChecks),
                riskFindings,
                analysisResponse.getSummary(),
                analysisResponse.getKeyFindings(),
                analysisResponse.getRiskFlags(),
                analysisResponse.getDoctorQuestions(),
                analysisResponse.getNextSteps(),
                redFlagReport
        );

        return new ReportAnalysisResult(analysisResponse, redFlagReport, context);
    }

    public List<String> answerQuestion(ReportContext context, String question) {
        String abnormalText = context.getAbnormalChecks() == null || context.getAbnormalChecks().isEmpty()
                ? "- None detected."
                : context.getAbnormalChecks().stream()
                .map(this::formatCheck)
                .collect(Collectors.joining("\n"));

        String riskText = context.getRiskFindings() == null || context.getRiskFindings().isEmpty()
                ? "- None detected."
                : context.getRiskFindings().stream().map(item -> "- " + item).collect(Collectors.joining("\n"));

        String summaryText = formatList(context.getSummary());
        String findingsText = formatList(context.getKeyFindings());
        String riskFlagsText = formatList(context.getRiskFlags());
        String questionsText = formatList(context.getDoctorQuestions());
        String nextStepsText = formatList(context.getNextSteps());
        String redFlagText = formatRedFlag(context.getRedFlagReport());

        String prompt = """
        You are a medical assistant AI.

        Use the analyzed report context below to answer the question.
        Respond in 2-3 short lines as a single concise paragraph.
        Do not use bullet points unless the user explicitly asks for them.
        If the user asks for a long or detailed answer, provide a longer response.
        If the user asks for a brief answer, keep it to 1-2 lines.
        If information is missing, say "Not found in report."

        REPORT SUMMARY:
        """ + summaryText + """

        KEY FINDINGS:
        """ + findingsText + """

        RISK FLAGS:
        """ + riskFlagsText + """

        QUESTIONS FOR DOCTOR:
        """ + questionsText + """

        NEXT STEPS:
        """ + nextStepsText + """

        RED FLAG ALERT:
        """ + redFlagText + """

        ABNORMAL PARAMETERS:
        """ + abnormalText + """

        RISK FINDINGS:
        """ + riskText + """

        REPORT:
        """ + (context.getReportText() == null ? "" : context.getReportText()) + """

        QUESTION:
        """ + question;

        String response = groqService.complete(null, prompt);
        String cleaned = response == null ? "" : response.trim();
        if (cleaned.isEmpty()) {
            return List.of("Not found in report.");
        }
        return List.of(cleaned);
    }

    public List<String> guidedInsights(ReportContext context, GuidedQuery query) {
        if (context == null) {
            return List.of();
        }
        return groqService.guidedInsights(context.getReportText(), query);
    }

    private List<RangeCheck> abnormalChecks(List<RangeCheck> checks) {
        if (checks == null) {
            return List.of();
        }
        return checks.stream()
                .filter(check -> check.getStatus() == RangeCheck.Status.HIGH || check.getStatus() == RangeCheck.Status.LOW)
                .collect(Collectors.toList());
    }

    private String formatCheck(RangeCheck check) {
        String range = "";
        if (check.getMin() != null || check.getMax() != null) {
            range = " (ref " + (check.getMin() == null ? "?" : check.getMin())
                    + "-" + (check.getMax() == null ? "?" : check.getMax()) + ")";
        }
        return "- " + check.getName() + " " + check.getValue() + " "
                + (check.getUnit() == null ? "" : check.getUnit()) + " "
                + check.getStatus() + range;
    }

    private String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- Not found in report.";
        }
        return items.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> "- " + item.trim())
                .collect(Collectors.joining("\n"));
    }

    private String formatRedFlag(RedFlagReport report) {
        if (report == null || !report.isActive()) {
            return "- None detected.";
        }
        String findings = formatList(report.getCriticalFindings());
        String instructions = formatList(report.getInstructions());
        return "ACTIVE\nCRITICAL FINDINGS:\n" + findings + "\nINSTRUCTIONS:\n" + instructions;
    }
}
