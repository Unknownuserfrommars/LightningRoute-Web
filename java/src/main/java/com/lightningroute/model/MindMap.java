package com.lightningroute.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindMap {
    private String rootNodeId;
    @Builder.Default
    private List<MindMapNode> nodes = new ArrayList<>();
    private String title;
    private String description;
    
    public void addNode(MindMapNode node) {
        nodes.add(node);
    }
    
    public MindMapNode getNodeById(String id) {
        return nodes.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    public void connectNodes(String sourceId, String targetId, String relationship) {
        MindMapNode sourceNode = getNodeById(sourceId);
        if (sourceNode != null) {
            MindMapNode.MindMapEdge edge = new MindMapNode.MindMapEdge(targetId, relationship);
            sourceNode.getConnections().add(edge);
        }
    }
}
