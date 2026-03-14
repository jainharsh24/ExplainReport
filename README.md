# ExplainReport Agentic AI

This project converts the PDF report analyzer into a simple Agentic AI workflow that uses tools to extract values, check ranges, detect risks, and generate a patient-friendly summary. The UI and upload flow remain unchanged.

**Agent Architecture**
This is a lightweight LangGraph-style workflow to keep the structure simple.
1. Agent Controller (`AgentService`) orchestrates steps and maintains state.
2. Planner (`AgentPlanner`) decides the next tool based on missing state.
3. Tools (`ReadPdfTool`, `ExtractParametersTool`, `CheckNormalRangesTool`, `DetectHealthRiskTool`, `GenerateSummaryTool`) do the work.
4. LLM Client (`GroqService`) powers risk explanations and summaries.

**Tool Definitions**
- `read_pdf()` -> Extract text from the uploaded PDF.
- `extract_parameters()` -> Identify medical parameters and values.
- `check_normal_ranges()` -> Compare extracted values with predefined ranges.
- `detect_health_risk()` -> Identify abnormal parameters and possible risks.
- `generate_summary()` -> Produce a clear explanation for the user.

**Updated Backend Structure**
- `dashboard/agent/AgentService.java` orchestrates the workflow.
- `dashboard/agent/AgentPlanner.java` decides which tool runs next.
- `dashboard/agent/tools/*` contains the five tool classes.
- `dashboard/agent/model/*` contains parameter and range models.
- `dashboard/service/GroqService.java` provides LLM calls.
- `dashboard/controller/DashboardController.java` uses the agent pipeline while keeping UI endpoints unchanged.

**Example Workflow**
1. Agent receives the PDF upload.
2. `read_pdf()` extracts text.
3. `extract_parameters()` finds lab values.
4. `check_normal_ranges()` flags abnormal values.
5. `detect_health_risk()` summarizes risks from abnormalities and red flags.
6. `generate_summary()` produces summary, key findings, risk flags, questions, and next steps.
7. Follow-up questions use the analyzed context from the agent state.

# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.2/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.2/maven-plugin/build-image.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.2/reference/using/devtools.html)
* [Thymeleaf](https://docs.spring.io/spring-boot/4.0.2/reference/web/servlet.html#web.servlet.spring-mvc.template-engines)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.2/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Handling Form Submission](https://spring.io/guides/gs/handling-form-submission/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.
