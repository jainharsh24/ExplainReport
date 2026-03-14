package com.harsh.explainreport.dashboard.controller;

import com.harsh.explainreport.dashboard.analysis.ReportAnalysisResult;
import com.harsh.explainreport.dashboard.analysis.ReportAnalysisService;
import com.harsh.explainreport.dashboard.analysis.ReportContext;
import com.harsh.explainreport.dashboard.dto.AnalysisResponse;
import com.harsh.explainreport.dashboard.dto.ChatRequest;
import com.harsh.explainreport.dashboard.dto.ChatResponse;
import com.harsh.explainreport.dashboard.dto.GuidedQuery;
import com.harsh.explainreport.dashboard.dto.InsightRequest;
import com.harsh.explainreport.dashboard.dto.InsightResponse;
import com.harsh.explainreport.dashboard.exception.PdfScanException;
import com.harsh.explainreport.dashboard.service.ExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class DashboardController {

    private final ExportService exportService;
    private final ReportAnalysisService reportAnalysisService;

    public DashboardController(ExportService exportService, ReportAnalysisService reportAnalysisService) {
        this.exportService = exportService;
        this.reportAnalysisService = reportAnalysisService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        clearSessionReport(session);
        return "index";
    }

    @PostMapping("/clear")
    public String clear(HttpSession session) {
        clearSessionReport(session);
        return "redirect:/";
    }

    @PostMapping("/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile file, Model model, HttpSession session) {
        if (file == null || file.isEmpty()) {
            clearSessionReport(session);
            model.addAttribute("scanError", "Please select a PDF to analyze.");
            return "index";
        }

        ReportAnalysisResult runResult;
        try {
            runResult = reportAnalysisService.analyzeReport(file);
        } catch (PdfScanException e) {
            clearSessionReport(session);
            model.addAttribute("scanError", e.getMessage());
            return "index";
        } catch (RuntimeException e) {
            clearSessionReport(session);
            model.addAttribute("scanError", "Not able to scan the PDF. Please upload a clearer report.");
            return "index";
        }

        session.setAttribute("reportContext", runResult.getContext());

        var redFlagReport = runResult.getRedFlagReport();
        session.setAttribute("redFlagActive", redFlagReport != null && redFlagReport.isActive());
        session.setAttribute("redFlagFindings", redFlagReport == null ? null : redFlagReport.getCriticalFindings());
        session.setAttribute("redFlagInstructions", redFlagReport == null ? null : redFlagReport.getInstructions());
        session.setAttribute("redFlagSummary", redFlagReport == null ? null : redFlagReport.getAlertSummary());

        AnalysisResponse result = runResult.getAnalysisResponse();
        if (result == null) {
            result = new AnalysisResponse(List.of(), List.of(), List.of(), List.of(), List.of());
        }
        session.setAttribute("summary", result.getSummary());
        session.setAttribute("keyFindings", result.getKeyFindings());
        session.setAttribute("riskFlags", result.getRiskFlags());
        session.setAttribute("questions", result.getDoctorQuestions());
        session.setAttribute("nextSteps", result.getNextSteps());

        model.addAttribute("redFlagActive", redFlagReport != null && redFlagReport.isActive());
        model.addAttribute("redFlagFindings", redFlagReport == null ? null : redFlagReport.getCriticalFindings());
        model.addAttribute("redFlagInstructions", redFlagReport == null ? null : redFlagReport.getInstructions());
        model.addAttribute("redFlagSummary", redFlagReport == null ? null : redFlagReport.getAlertSummary());
        model.addAttribute("summary", result.getSummary());
        model.addAttribute("keyFindings", result.getKeyFindings());
        model.addAttribute("riskFlags", result.getRiskFlags());
        model.addAttribute("questions", result.getDoctorQuestions());
        model.addAttribute("nextSteps", result.getNextSteps());

        return "index";
    }


    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, HttpSession session) {

        ReportContext context = (ReportContext) session.getAttribute("reportContext");
        if (context == null || context.getReportText() == null || context.getReportText().isBlank()) {
            return ResponseEntity.badRequest().body(ChatResponse.error("Please upload a report first."));
        }

        String question = request == null ? null : request.getQuestion();
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(ChatResponse.error("Please enter a question."));
        }

        var answer = reportAnalysisService.answerQuestion(context, question);

        return ResponseEntity.ok(ChatResponse.success(answer));
    }

    @PostMapping(path = "/insight", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<InsightResponse> insight(@RequestBody InsightRequest request, HttpSession session) {
        ReportContext context = (ReportContext) session.getAttribute("reportContext");
        if (context == null || context.getReportText() == null || context.getReportText().isBlank()) {
            return ResponseEntity.badRequest().body(InsightResponse.error("Please upload a report first."));
        }

        String type = request == null ? null : request.getType();
        GuidedQuery query = GuidedQuery.fromType(type);
        if (query == null) {
            return ResponseEntity.badRequest().body(InsightResponse.error("Invalid query type."));
        }

        var items = reportAnalysisService.guidedInsights(context, query);
        session.setAttribute("lastInsightTitle", query.getTitle());
        session.setAttribute("lastInsightItems", items);
        return ResponseEntity.ok(InsightResponse.success(query.getTitle(), items));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam("format") String format, HttpSession session) {
        var summary = exportService.getList(session, "summary");
        var keyFindings = exportService.getList(session, "keyFindings");
        var riskFlags = exportService.getList(session, "riskFlags");
        var questions = exportService.getList(session, "questions");
        var nextSteps = exportService.getList(session, "nextSteps");
        var lastInsightTitle = exportService.getString(session, "lastInsightTitle");
        var lastInsightItems = exportService.getList(session, "lastInsightItems");
        var redFlagActive = exportService.getBoolean(session, "redFlagActive");
        var redFlagFindings = exportService.getList(session, "redFlagFindings");
        var redFlagInstructions = exportService.getList(session, "redFlagInstructions");

        if (exportService.isAllEmpty(summary, keyFindings, riskFlags, questions, nextSteps, lastInsightItems, redFlagFindings)) {
            return ResponseEntity.badRequest().build();
        }

        String exportText = exportService.buildExportText(
                summary,
                keyFindings,
                riskFlags,
                questions,
                nextSteps,
                redFlagActive,
                redFlagFindings,
                redFlagInstructions,
                lastInsightTitle,
                lastInsightItems
        );

        if ("txt".equalsIgnoreCase(format)) {
            byte[] body = exportService.toTextBytes(exportText);
            return exportService.textResponse(body, "explainreport.txt");
        }

        byte[] body = exportService.toPdfBytes(exportText);
        return exportService.pdfResponse(body, "explainreport.pdf");
    }

    private void clearSessionReport(HttpSession session) {
        session.removeAttribute("reportContext");
        session.removeAttribute("summary");
        session.removeAttribute("keyFindings");
        session.removeAttribute("riskFlags");
        session.removeAttribute("questions");
        session.removeAttribute("nextSteps");
        session.removeAttribute("lastInsightTitle");
        session.removeAttribute("lastInsightItems");
        session.removeAttribute("redFlagActive");
        session.removeAttribute("redFlagFindings");
        session.removeAttribute("redFlagInstructions");
        session.removeAttribute("redFlagSummary");
    }
}
