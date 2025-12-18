package net.frostimpact.rpgclasses_v2.rpgclass;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an RPG class (e.g., Warrior, Mage, Rogue)
 */
public class RPGClass {
    private final String id;
    private final String name;
    private final String description;
    private final Map<StatType, List<StatModifier>> baseStats;
    private final String skillTreeId;
    
    public RPGClass(String id, String name, String description, String skillTreeId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.skillTreeId = skillTreeId;
        this.baseStats = new HashMap<>();
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
    
    public String getSkillTreeId() {
        return skillTreeId;
    }
    
    /**
     * Add a base stat modifier for this class
     */
    public void addBaseStat(StatType statType, StatModifier modifier) {
        baseStats.computeIfAbsent(statType, k -> new ArrayList<>()).add(modifier);
    }
    
    /**
     * Get all base stat modifiers for a specific stat type
     */
    public List<StatModifier> getBaseStats(StatType statType) {
        return baseStats.getOrDefault(statType, new ArrayList<>());
    }
    
    /**
     * Get all base stat modifiers
     */
    public Map<StatType, List<StatModifier>> getAllBaseStats() {
        return new HashMap<>(baseStats);
    }
}
