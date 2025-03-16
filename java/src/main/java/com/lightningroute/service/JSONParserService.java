package com.lightningroute.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightningroute.model.MindMap;
import com.lightningroute.model.MindMapNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class JSONParserService {

    private final ObjectMapper objectMapper;

    /**
     * Extract JSON content from a string that might contain non-JSON text
     * 
     * @param content The content that may contain JSON
     * @return The extracted JSON string
     */
    public String extractJsonFromString(String content) {
        // Pattern to find a JSON object in the text
        Pattern pattern = Pattern.compile("\\{[^{}]*((\\{[^{}]*\\})[^{}]*)*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(0);
        }
        
        return null;
    }

    /**
     * Parse OpenAI response to MindMap object
     * 
     * @param response The OpenAI API response
     * @return MindMap object
     */
    public MindMap parseJsonToMindMap(String response) {
        String jsonContent = extractJsonFromString(response);
        if (jsonContent == null) {
            log.warn("Could not extract JSON from response");
            return null;
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // Create mind map
            MindMap mindMap = new MindMap();
            mindMap.setTitle(getStringOrDefault(rootNode, "title", "Mind Map"));
            mindMap.setDescription(getStringOrDefault(rootNode, "description", "Generated from text"));
            
            // Process nodes
            JsonNode nodesNode = rootNode.get("nodes");
            if (nodesNode != null && nodesNode.isArray()) {
                for (JsonNode nodeJson : nodesNode) {
                    MindMapNode node = createNodeFromJson(nodeJson);
                    mindMap.addNode(node);
                    
                    // Set root node if this is marked as the root
                    if ("root".equals(node.getCategory())) {
                        mindMap.setRootNodeId(node.getId());
                    }
                }
            }
            
            // Process connections if they are separate from nodes
            JsonNode connectionsNode = rootNode.get("connections");
            if (connectionsNode != null && connectionsNode.isArray()) {
                processConnections(mindMap, connectionsNode);
            }
            
            return mindMap;
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON response", e);
            return null;
        }
    }
    
    /**
     * Create a MindMapNode from JSON
     * 
     * @param nodeJson The node JSON
     * @return MindMapNode object
     */
    private MindMapNode createNodeFromJson(JsonNode nodeJson) {
        String id = getStringOrDefault(nodeJson, "id", UUID.randomUUID().toString());
        String label = getStringOrDefault(nodeJson, "label", "Unnamed Node");
        String category = getStringOrDefault(nodeJson, "category", "concept");
        String tooltip = getStringOrDefault(nodeJson, "tooltip", "");
        Integer level = getIntOrDefault(nodeJson, "level", 1);
        
        MindMapNode node = new MindMapNode();
        node.setId(id);
        node.setLabel(label);
        node.setCategory(category);
        node.setTooltip(tooltip);
        node.setLevel(level);
        node.setConnections(new ArrayList<>());
        
        // Process connections embedded in node
        JsonNode connectionsNode = nodeJson.get("connections");
        if (connectionsNode != null && connectionsNode.isArray()) {
            for (JsonNode connJson : connectionsNode) {
                String target = getStringOrDefault(connJson, "target", "");
                String relationship = getStringOrDefault(connJson, "relationship", "related");
                
                if (!target.isEmpty()) {
                    MindMapNode.MindMapEdge edge = new MindMapNode.MindMapEdge();
                    edge.setTarget(target);
                    edge.setRelationship(relationship);
                    node.getConnections().add(edge);
                }
            }
        }
        
        return node;
    }
    
    /**
     * Process connections from a separate array
     * 
     * @param mindMap The mind map to add connections to
     * @param connectionsNode JSON array of connections
     */
    private void processConnections(MindMap mindMap, JsonNode connectionsNode) {
        for (JsonNode connJson : connectionsNode) {
            String source = getStringOrDefault(connJson, "source", "");
            String target = getStringOrDefault(connJson, "target", "");
            String relationship = getStringOrDefault(connJson, "relationship", "related");
            
            if (!source.isEmpty() && !target.isEmpty()) {
                mindMap.connectNodes(source, target, relationship);
            }
        }
    }
    
    /**
     * Get a string from JSON with a default fallback
     * 
     * @param node The JSON node
     * @param fieldName The field name
     * @param defaultValue Default value if field doesn't exist
     * @return The string value
     */
    private String getStringOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : defaultValue;
    }
    
    /**
     * Get an integer from JSON with a default fallback
     * 
     * @param node The JSON node
     * @param fieldName The field name
     * @param defaultValue Default value if field doesn't exist
     * @return The integer value
     */
    private Integer getIntOrDefault(JsonNode node, String fieldName, Integer defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && field.isInt()) ? field.asInt() : defaultValue;
    }
}
