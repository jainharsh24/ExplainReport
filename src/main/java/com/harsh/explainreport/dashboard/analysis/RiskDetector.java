package com.harsh.explainreport.dashboard.analysis;

import com.harsh.explainreport.dashboard.dto.RedFlagReport;
import com.harsh.explainreport.dashboard.service.GroqService;
import com.harsh.explainreport.dashboard.util.TextParsing;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiskDetector {

    private final GroqService groqService;

    public RiskDetector(GroqService groqService) {
        this.groqService = groqService;
    }

    public List<String> detectHealthRisk(String reportText, List<RangeCheck> rangeChecks, RedFlagReport redFlagReport) {
        List<RangeCheck> abnormalities = rangeChecks == null ? List.of() : rangeChecks.stream()
                .filter(check -> check.getStatus() == RangeCheck.Status.HIGH || check.getStatus() == RangeCheck.Status.LOW)
                .collect(Collectors.toList());

        String abnormalText = abnormalities.isEmpty()
                ? "- None detected."
                : abnormalities.stream()
                .map(this::formatCheck)
                .collect(Collectors.joining("\n"));

        String redFlags = redFlagReport == null || redFlagReport.getCriticalFindings().isEmpty()
                ? "- None detected."
                : redFlagReport.getCriticalFindings().stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));

        String systemPrompt = "You are a careful medical assistant. "
                + "You identify possible health risks based on abnormal lab values. "
                + "Do not diagnose. Mention uncertainty. Keep bullets concise.";

        String userPrompt = "ABNORMAL PARAMETERS:\n" + abnormalText
                + "\n\nCRITICAL RED FLAGS:\n" + redFlags
                + "\n\nProvide 3-6 bullet points of possible health risks or concerns. "
                + "If none, say that no clear risks were detected from the parameters.";

        String response = groqService.complete(systemPrompt, userPrompt);
        List<String> items = TextParsing.parseList(response);
        if (items.isEmpty()) {
            if (abnormalities.isEmpty() && (redFlagReport == null || redFlagReport.getCriticalFindings().isEmpty())) {
                return List.of("No clear risks detected from extracted parameters.");
            }
            return List.of("Potential risks are suggested by abnormal parameters; discuss with a clinician.");
        }

        return items.stream().limit(6).collect(Collectors.toList());
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
