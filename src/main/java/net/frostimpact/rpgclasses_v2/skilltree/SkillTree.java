package net.frostimpact.rpgclasses_v2.skilltree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a complete skill tree for a class
 */
public class SkillTree {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, SkillNode> nodes;
    
    public SkillTree(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = new HashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Add a skill node to this tree
     */
    public void addNode(SkillNode node) {
        nodes.put(node.getId(), node);
    }
    
    /**
     * Get a skill node by ID
     */
    public Optional<SkillNode> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }
    
    /**
     * Get all skill nodes in this tree
     */
    public List<SkillNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    /**
     * Check if a skill node exists
     */
    public boolean hasNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }
}
