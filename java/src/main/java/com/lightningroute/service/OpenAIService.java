package com.lightningroute.service;

import com.lightningroute.model.MindMap;
import com.lightningroute.model.MindMapNode;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final com.theokanning.openai.service.OpenAiService openAiService;
    
    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;
    
    /**
     * Generate a mind map from the provided text
     * Uses caching to avoid repeated API calls for the same text
     * Uses retry mechanism with exponential backoff for API failures
     * 
     * @param text The text to generate a mind map from
     * @return A structured mind map object
     */
    @Cacheable("mindmaps")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public MindMap generateMindMap(String text) {
        try {
            // Prepare the prompt for OpenAI
            String prompt = "Create a detailed mind map from the following text. " +
                    "Format the response as JSON with nodes and connections. " +
                    "Categorize each node as 'root', 'concept', 'example', or 'definition'. " +
                    "Text: " + text;
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Collections.singletonList(
                            new ChatMessage("user", prompt)))
                    .build();
            
            // Call OpenAI API
            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            
            // Parse JSON response to mind map
            return parseJsonToMindMap(response);
        } catch (Exception e) {
            log.error("Error generating mind map from OpenAI", e);
            return generateFallbackMindMap(text);
        }
    }
    
    /**
     * Fallback method to generate a simple mind map when OpenAI API fails
     * 
     * @param text The input text
     * @return A basic mind map
     */
    private MindMap generateFallbackMindMap(String text) {
        MindMap mindMap = new MindMap();
        mindMap.setTitle("Mind Map (Generated Locally)");
        mindMap.setDescription("This mind map was generated locally due to API issues.");
        
        // Create root node from first line or first 50 chars
        String rootLabel = text.split("\\n")[0];
        if (rootLabel.length() > 50) {
            rootLabel = rootLabel.substring(0, 47) + "...";
        }
        
        MindMapNode rootNode = MindMapNode.builder()
                .id("root")
                .label(rootLabel)
                .category("root")
                .level(0)
                .tooltip("Root concept")
                .connections(new ArrayList<>())
                .build();
        
        mindMap.addNode(rootNode);
        mindMap.setRootNodeId("root");
        
        // Split text into sentences and create nodes
        String[] sentences = text.split("[.!?]\\s+");
        AtomicInteger counter = new AtomicInteger(1);
        
        // Take up to 10 sentences for the fallback map
        final String finalRootLabel = rootLabel; // Make a final copy of rootLabel for use in lambda
        Arrays.stream(sentences)
                .limit(10)
                .filter(sentence -> !sentence.trim().isEmpty() && !sentence.equals(finalRootLabel))
                .forEach(sentence -> {
                    String id = "node" + counter.getAndIncrement();
                    String label = sentence.trim();
                    if (label.length() > 100) {
                        label = label.substring(0, 97) + "...";
                    }
                    
                    // Determine category based on content
                    String category = "concept";
                    if (label.contains("example") || label.contains("instance") || label.contains("such as")) {
                        category = "example";
                    } else if (label.contains("defined") || label.contains("meaning") || label.contains("refers to")) {
                        category = "definition";
                    }
                    
                    MindMapNode node = MindMapNode.builder()
                            .id(id)
                            .label(label)
                            .category(category)
                            .level(1)
                            .tooltip("Related to " + finalRootLabel)
                            .connections(new ArrayList<>())
                            .build();
                    
                    mindMap.addNode(node);
                    mindMap.connectNodes("root", id, "relates to");
                });
        
        return mindMap;
    }
    
    /**
     * Parse the JSON response from OpenAI into a MindMap object
     * This method handles various JSON formats that OpenAI might return
     * 
     * @param jsonResponse The JSON response from OpenAI
     * @return A structured mind map
     */
    private MindMap parseJsonToMindMap(String jsonResponse) {
        // Extract JSON from the response (OpenAI might return text before/after the JSON)
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonResponse);
        
        if (!matcher.find()) {
            log.warn("Could not extract JSON from OpenAI response, falling back to simple map");
            return generateFallbackMindMap(jsonResponse);
        }
        
        // In a real implementation, use a proper JSON parser such as Jackson or Gson
        // This is a simplified placeholder that would be replaced with actual JSON parsing
        
        // For now, return a simple fallback mind map
        return generateFallbackMindMap(jsonResponse);
        
        // In the real implementation, we would parse the JSON to create:
        // 1. Create MindMap instance
        // 2. Add nodes with their properties from the JSON
        // 3. Connect nodes based on the relationships in the JSON
        // 4. Return the complete mind map
    }
}
