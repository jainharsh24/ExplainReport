package com.harsh.explainreport.dashboard.analysis;

import com.harsh.explainreport.dashboard.dto.AnalysisResponse;
import com.harsh.explainreport.dashboard.dto.RedFlagReport;
import com.harsh.explainreport.dashboard.service.GroqService;
import com.harsh.explainreport.dashboard.util.TextParsing;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SummaryGenerator {

    private final GroqService groqService;

    public SummaryGenerator(GroqService groqService) {
        this.groqService = groqService;
    }

    public AnalysisResponse generateSummary(String reportText,
                                            List<RangeCheck> rangeChecks,
                                            List<String> riskFindings,
                                            RedFlagReport redFlagReport) {

        String abnormalities = rangeChecks == null ? "- None" : rangeChecks.stream()
                .filter(check -> check.getStatus() == RangeCheck.Status.HIGH || check.getStatus() == RangeCheck.Status.LOW)
                .map(this::formatCheck)
                .collect(Collectors.joining("\n"));

        if (abnormalities.isBlank()) {
            abnormalities = "- None";
        }

        String riskText = riskFindings == null || riskFindings.isEmpty()
                ? "- None"
                : riskFindings.stream().map(item -> "- " + item).collect(Collectors.joining("\n"));

        String redFlags = redFlagReport == null || redFlagReport.getCriticalFindings().isEmpty()
                ? "- None"
                : redFlagReport.getCriticalFindings().stream().map(item -> "- " + item).collect(Collectors.joining("\n"));

        String prompt = """
        Return output STRICTLY in this format:

        SUMMARY:
        - <bullet point>
        - <bullet point>

        KEY FINDINGS:
        - <bullet point>
        - <bullet point>

        RISK FLAGS:
        - <bullet point>
        - <bullet point>

        QUESTIONS:
        1) <question>
        2) <question>
        3) <question>
        4) <question>
        5) <question>

        NEXT STEPS:
        - <bullet point>
        - <bullet point>

        Use the extracted abnormalities and risks below when possible.
        Avoid diagnosis. Mention that ranges can vary by lab.

        ABNORMAL PARAMETERS:
        """ + abnormalities + """

        RISK FINDINGS:
        """ + riskText + """

        CRITICAL RED FLAGS:
        """ + redFlags + """

        REPORT:
        """ + reportText;

        String response = groqService.complete("You summarize medical reports clearly for patients.", prompt);

        String summarySection = extractSection(response, "SUMMARY:", "KEY FINDINGS:");
        String keyFindingsSection = extractSection(response, "KEY FINDINGS:", "RISK FLAGS:");
        String riskSection = extractSection(response, "RISK FLAGS:", "QUESTIONS:");
        String questionsSection = extractSection(response, "QUESTIONS:", "NEXT STEPS:");
        String nextStepsSection = extractSection(response, "NEXT STEPS:", null);

        return new AnalysisResponse(
                TextParsing.parseList(summarySection),
                TextParsing.parseList(keyFindingsSection),
                TextParsing.parseList(riskSection),
                TextParsing.parseList(questionsSection),
                TextParsing.parseList(nextStepsSection)
        );
    }

    private String extractSection(String response, String startHeader, String endHeader) {
        if (response == null || response.isBlank()) {
            return "";
        }

        int startIndex = response.indexOf(startHeader);
        if (startIndex < 0) {
            return "";
        }
        startIndex += startHeader.length();

        int endIndex = endHeader == null ? -1 : response.indexOf(endHeader, startIndex);
        String section = endIndex < 0 ? response.substring(startIndex) : response.substring(startIndex, endIndex);
        return section.trim();
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
}
