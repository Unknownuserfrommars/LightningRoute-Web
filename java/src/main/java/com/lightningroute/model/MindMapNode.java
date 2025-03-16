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
public class MindMapNode {
    private String id;
    private String label;
    private String category; // e.g., "root", "concept", "example", "definition"
    private String tooltip;
    private Integer level;
    @Builder.Default
    private List<MindMapEdge> connections = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MindMapEdge {
        private String target;
        private String relationship;
    }
}
