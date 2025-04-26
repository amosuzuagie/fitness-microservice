package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);

//        log.info("RESPONSE FROM AI: {}", aiResponse);

        return processAiResponse(activity, aiResponse);
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("```\\n", "")
                    .trim();

//            log.info("PARSED RESPONSE FROM AI: {}", jsonContent);

            // Parsing Analysis
            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");

            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall: ");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace:");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "HeartRate:");
            addAnalysisSection(fullAnalysis, analysisNode, "calories", "CaloriesBurned:");

            // Parsing Improvements
            List<String> improvements = extractImprovement(analysisJson.path("improvements"));

            // Parsing Suggestions
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));

            // Parsing Safety
            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private List<String> extractImprovement(JsonNode improvementNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementNode.isArray()) {
            improvementNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s %s", area, detail));
            });
        }
        return improvements.isEmpty()
                ? Collections.singletonList("No specific improvement provided")
                : improvements;
    }

    private List<String> extractSuggestions(JsonNode suggestionNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionNode.isArray()) {
            suggestionNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s %s", workout, description));
            });
        }
        return suggestions.isEmpty()
                ? Collections.singletonList("No specific suggestion provided")
                : suggestions;
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if (safetyNode.isArray()){
            safetyNode.forEach(item -> {
                safety.add(item.asText());
            });
        }
        return safety.isEmpty()
                ? Collections.singletonList("Follow general safety guidelines.")
                : safety;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if (!analysisNode.path(key).isMissingNode()) {
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting a professional"))
                .safety(Arrays.asList(
                        "Warm up properly before each run with dynamic stretches like leg swings and arm circles.",
                        "Cool down after each run with static stretches, holding each stretch for 30 seconds.",
                        "Stay hydrated by drinking plenty of water before, during, and after your runs.",
                        "Listen to your body and take rest days when needed to prevent injuries."
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String createPromptForActivity(Activity activity) {
        return String.format(
                """
                Analyze this fitness activity and provide detailed recommendation in the following EXACT JSON format:
                {
                  "analysis":{
                  "overall": "Overall analysis here",
                  "pace": "Pace analysis here",
                  "heartRate": "HeartRate analysis here",
                  "caloriesBurned": "Calories analysis here"
                  },
                  "improvements": [
                    {
                    "area": "Area name",
                    "recommendation": "Detailed recommendation
                    }
                  ],
                  "suggestions": [
                    {
                      "workout": "Workout name",
                      "description": "Detailed workout description"
                    }
                  ],
                  "safety": [
                    "Safety point 1",
                    "Safety point 2
                  ]
                }
                
                Analyze this activity:
                Activity Type: %s
                Duration: %d minutes
                Calories Burned: %d
                Additional Metrics: %s
                
                provide detailed analysis focusing on performance, improvements, next workout suggestions and safety guidelines.
                Ensure the response follows the EXACT JSON format shown above.
                """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }
}
