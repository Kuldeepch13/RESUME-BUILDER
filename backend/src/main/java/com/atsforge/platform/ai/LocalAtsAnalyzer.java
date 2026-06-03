package com.atsforge.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LocalAtsAnalyzer {
    private final ObjectMapper mapper;
    public LocalAtsAnalyzer(ObjectMapper mapper) { this.mapper = mapper; }

    public JsonNode analyze(String resumeJson, String jobDescription) {
        Set<String> resumeWords = words(resumeJson);
        Set<String> jobWords = words(jobDescription == null ? "" : jobDescription);
        long matched = jobWords.stream().filter(resumeWords::contains).count();
        int match = jobWords.isEmpty() ? 70 : (int) Math.min(100, (matched * 100L) / jobWords.size());
        int score = Math.min(95, 45 + Math.min(25, resumeWords.size() / 4) + match / 4);
        ObjectNode response = mapper.createObjectNode().put("atsScore", score).put("matchPercentage", match);
        ArrayNode missing = response.putArray("missingKeywords");
        jobWords.stream().filter(word -> !resumeWords.contains(word)).limit(10).forEach(missing::add);
        response.putArray("suggestions")
                .add("Quantify outcomes in experience bullets with metrics.")
                .add("Mirror relevant job-description terminology naturally.")
                .add("Keep headings conventional for ATS parsing.");
        response.putArray("strongSections").add("Readable structured content");
        response.putArray("weakSections").add("Review keyword coverage and measurable achievements");
        return response;
    }

    private Set<String> words(String input) {
        return Arrays.stream(input.toLowerCase().replaceAll("[^a-z0-9+# ]", " ").split("\\s+"))
                .filter(word -> word.length() > 2).collect(Collectors.toSet());
    }
}

