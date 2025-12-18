package net.frostimpact.rpgclasses_v2.skilltree;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single node in a skill tree
 */
public class SkillNode {
    private final String id;
    private final String name;
    private final String description;
    private final int maxLevel;
    private final int pointCost;
    private final List<String> requirements; // IDs of required skills
    private final int requiredLevel; // Player level requirement
    
    public SkillNode(String id, String name, String description, int maxLevel, int pointCost, int requiredLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.pointCost = pointCost;
        this.requiredLevel = requiredLevel;
        this.requirements = new ArrayList<>();
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
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public int getPointCost() {
        return pointCost;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public List<String> getRequirements() {
        return requirements;
    }
    
    public void addRequirement(String skillId) {
        requirements.add(skillId);
    }
}
