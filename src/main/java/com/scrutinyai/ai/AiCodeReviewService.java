package com.scrutinyai.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    
    public AiReviewResult reviewCode(String language, String code) {
        String prompt = buildPrompt(language, code);
        JsonNode schema = readSchema(RESPONSE_SCHEMA_JSON);

        String rawJson = aiClient.generateStructuredContent(prompt, schema);

        try {
            JsonNode result = objectMapper.readTree(rawJson);
            return parseResult(result);
        } catch (IOException e) {
            throw new IllegalStateException("Gemini cevabı parse edilemedi: " + rawJson, e);
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
            throw new IllegalStateException("Gemini cevabı parse edilemedi: " + rawJson, e);
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
            throw new IllegalStateException("Gemini cevabı parse edilemedi: " + rawJson, e);
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
            throw new IllegalStateException("Gemini cevabı parse edilemedi: " + rawJson, e);
        }
    }

    private String buildGenerateTestsPrompt(Review review) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/generate-tests.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{codeSnippet}", review.getCodeSnippet());
        } catch (IOException e) {
            throw new IllegalStateException("Prompt dosyası okunamadı", e);
        }
    }
    
    private String buildFixPrompt(Review review, Issue issue) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/fix-code.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{codeSnippet}", review.getCodeSnippet())
                    .replace("{severity}", issue.getSeverity().name())
                    .replace("{lineNumber}", String.valueOf(issue.getLineNumber()))
                    .replace("{title}", issue.getTitle())
                    .replace("{description}", issue.getDescription())
                    .replace("{suggestion}", issue.getSuggestion() != null ? issue.getSuggestion() : "");
        } catch (IOException e) {
            throw new IllegalStateException("Prompt dosyası okunamadı", e);
        }
    }

    private String buildPrompt(String language, String code) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/code-review.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return template.replace("{language}", language).replace("{code}", code);
        } catch (IOException e) {
            throw new IllegalStateException("Prompt dosyası okunamadı", e);
        }
    }

    private String buildExplainPrompt(Review review, Issue issue) {
        try {
            String template = new String(
                    new ClassPathResource("prompts/explain-issue.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return template
                    .replace("{language}", review.getLanguage())
                    .replace("{codeSnippet}", review.getCodeSnippet())
                    .replace("{severity}", issue.getSeverity().name())
                    .replace("{lineNumber}", String.valueOf(issue.getLineNumber()))
                    .replace("{title}", issue.getTitle())
                    .replace("{description}", issue.getDescription());
        } catch (IOException e) {
            throw new IllegalStateException("Prompt dosyası okunamadı", e);
        }
    }
    
    private JsonNode readSchema(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (IOException e) {
            throw new IllegalStateException("Response schema parse edilemedi", e);
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
                    issueNode.hasNonNull("suggestion") ? issueNode.path("suggestion").asText() : null
            ));
        }

        return new AiReviewResult(score, summary, issues);
    }
}