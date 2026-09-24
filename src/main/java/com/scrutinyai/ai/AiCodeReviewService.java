package com.scrutinyai.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinyai.entity.Issue;
import com.scrutinyai.entity.Review;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiCodeReviewService {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    private static final String RESPONSE_SCHEMA_JSON = """
            {
              "type": "OBJECT",
              "properties": {
                "score": {"type": "INTEGER"},
                "summary": {"type": "STRING"},
                "issues": {
                  "type": "ARRAY",
                  "items": {
                    "type": "OBJECT",
                    "properties": {
                      "severity": {"type": "STRING", "enum": ["LOW", "MEDIUM", "HIGH"]},
                      "lineNumber": {"type": "INTEGER"},
                      "title": {"type": "STRING"},
                      "description": {"type": "STRING"},
                      "suggestion": {"type": "STRING"}
                    },
                    "required": ["severity", "title", "description"]
                  }
                }
              },
              "required": ["score", "summary", "issues"]
            }
            """;

    private static final String EXPLAIN_SCHEMA_JSON = """
            {
              "type": "OBJECT",
              "properties": {
                "explanation": {"type": "STRING"}
              },
              "required": ["explanation"]
            }
            """;

    private static final String FIX_SCHEMA_JSON = """
            {
              "type": "OBJECT",
              "properties": {
                "fixedCode": {"type": "STRING"}
              },
              "required": ["fixedCode"]
            }
            """;

    private static final String GENERATE_TESTS_SCHEMA_JSON = """
            {
              "type": "OBJECT",
              "properties": {
                "testCode": {"type": "STRING"}
              },
              "required": ["testCode"]
            }
            """;

    private static final Map<String, String> TEST_FRAMEWORKS = Map.ofEntries(
            Map.entry("java", "JUnit 5 (with Mockito for mocking)"),
            Map.entry("kotlin", "JUnit 5 with Kotlin test idioms (with MockK for mocking)"),
            Map.entry("javascript", "Jest"),
            Map.entry("typescript", "Jest with TypeScript"),
            Map.entry("python", "pytest"),
            Map.entry("csharp", "xUnit (with Moq for mocking)"),
            Map.entry("go", "the standard library's testing package (with table-driven tests)"),
            Map.entry("sql", "a set of test queries with expected result assertions, described in comments"));

    public AiReviewResult reviewCode(String language, String code) {
        String prompt = buildPrompt(language, code);
        JsonNode schema = readSchema(RESPONSE_SCHEMA_JSON);

        String rawJson = aiClient.generateStructuredContent(prompt, schema);

        try {
            JsonNode result = objectMapper.readTree(rawJson);
            return parseResult(result);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + rawJson, e);
        }
    }

    public String explainIssue(Review review, Issue issue) {
        String prompt = buildExplainPrompt(review, issue);
        JsonNode schema = readSchema(EXPLAIN_SCHEMA_JSON);

        String rawJson = aiClient.generateStructuredContent(prompt, schema);

        try {
            JsonNode result = objectMapper.readTree(rawJson);
            return result.path("explanation").asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + rawJson, e);
        }
    }

    public String fixIssue(Review review, Issue issue) {
        String prompt = buildFixPrompt(review, issue);
        JsonNode schema = readSchema(FIX_SCHEMA_JSON);

        String rawJson = aiClient.generateStructuredContent(prompt, schema);

        try {
            JsonNode result = objectMapper.readTree(rawJson);
            return result.path("fixedCode").asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + rawJson, e);
        }
    }

    public String generateTests(Review review) {
        String prompt = buildGenerateTestsPrompt(review);
        JsonNode schema = readSchema(GENERATE_TESTS_SCHEMA_JSON);

        String rawJson = aiClient.generateStructuredContent(prompt, schema);

        try {
            JsonNode result = objectMapper.readTree(rawJson);
            return result.path("testCode").asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + rawJson, e);
        }
    }

    private String buildGenerateTestsPrompt(Review review) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/generate-tests.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{testFramework}", resolveTestFramework(review.getLanguage()))
                    .replace("{codeSnippet}", review.getCodeSnippet());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file", e);
        }
    }

    private String resolveTestFramework(String language) {
        return TEST_FRAMEWORKS.getOrDefault(language.toLowerCase(), "an appropriate testing framework for " + language);
    }

    private String buildFixPrompt(Review review, Issue issue) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/fix-code.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{codeSnippet}", review.getCodeSnippet())
                    .replace("{severity}", issue.getSeverity().name())
                    .replace("{lineNumber}", String.valueOf(issue.getLineNumber()))
                    .replace("{title}", issue.getTitle())
                    .replace("{description}", issue.getDescription())
                    .replace("{suggestion}", issue.getSuggestion() != null ? issue.getSuggestion() : "");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file", e);
        }
    }

    private String buildPrompt(String language, String code) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/code-review.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return template.replace("{language}", language).replace("{code}", code);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file", e);
        }
    }

    private String buildExplainPrompt(Review review, Issue issue) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/explain-issue.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{codeSnippet}", review.getCodeSnippet())
                    .replace("{severity}", issue.getSeverity().name())
                    .replace("{lineNumber}", String.valueOf(issue.getLineNumber()))
                    .replace("{title}", issue.getTitle())
                    .replace("{description}", issue.getDescription());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt file", e);
        }
    }

    private JsonNode readSchema(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse response schema", e);
        }
    }

    private AiReviewResult parseResult(JsonNode node) {
        int score = node.path("score").asInt();
        String summary = node.path("summary").asText();

        List<AiIssueResult> issues = new ArrayList<>();
        for (JsonNode issueNode : node.path("issues")) {
            issues.add(new AiIssueResult(
                    issueNode.path("severity").asText(),
                    issueNode.hasNonNull("lineNumber") ? issueNode.path("lineNumber").asInt() : null,
                    issueNode.path("title").asText(),
                    issueNode.path("description").asText(),
                    issueNode.hasNonNull("suggestion") ? issueNode.path("suggestion").asText() : null));
        }

        return new AiReviewResult(score, summary, issues);
    }
}