package net.frostimpact.rpgclasses_v2.armor;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single piece of custom armor.
 * Each piece can have stats and passives.
 * 
 * Example usage:
 * <pre>
 * CustomArmorPiece shadowHelmet = new CustomArmorPiece.Builder("shadow_helmet", "Shadow Helm", ArmorSlot.HELMET)
 *     .rarity(ArmorRarity.RARE)
 *     .defense(5)
 *     .addStat(StatType.MOVE_SPEED, 5)
 *     .passive(new ShadowVisionPassive())
 *     .build();
 * </pre>
 */
public class CustomArmorPiece {
    private final String id;
    private final String displayName;
    private final String description;
    private final ArmorSlot slot;
    private final ArmorRarity rarity;
    
    // Base defense value
    private final int defense;
    
    // Bonus stat modifiers granted when wearing the armor
    private final Map<StatType, Double> bonusStats;
    
    // Passive effects
    private final List<ArmorPassive> passives;
    
    // Lore lines for item tooltip
    private final List<String> loreLines;
    
    // Set ID this piece belongs to (null if not part of a set)
    private final String setId;
    
    private CustomArmorPiece(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.slot = builder.slot;
        this.rarity = builder.rarity;
        this.defense = builder.defense;
        this.bonusStats = new HashMap<>(builder.bonusStats);
        this.passives = new ArrayList<>(builder.passives);
        this.loreLines = new ArrayList<>(builder.loreLines);
        this.setId = builder.setId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ArmorSlot getSlot() { return slot; }
    public ArmorRarity getRarity() { return rarity; }
    
    /**
     * Get the effective defense (including rarity multiplier)
     */
    public int getEffectiveDefense() {
        return (int) (defense * rarity.getStatMultiplier());
    }
    
    /**
     * Get the raw base defense without rarity multiplier
     */
    public int getRawDefense() { return defense; }
    
    public Map<StatType, Double> getBonusStats() { return new HashMap<>(bonusStats); }
    public List<ArmorPassive> getPassives() { return new ArrayList<>(passives); }
    public List<String> getLoreLines() { return new ArrayList<>(loreLines); }
    public String getSetId() { return setId; }
    
    /**
     * Check if this armor piece is part of a set
     */
    public boolean isPartOfSet() {
        return setId != null && !setId.isEmpty();
    }
    
    /**
     * Check if this armor piece has any passives
     */
    public boolean hasPassives() {
        return !passives.isEmpty();
    }
    
    /**
     * Get the bonus stat value for a specific stat type
     */
    public double getBonusStat(StatType statType) {
        return bonusStats.getOrDefault(statType, 0.0);
    }
    
    /**
     * Create stat modifiers for all bonus stats on this armor piece
     */
    public List<StatModifier> createStatModifiers() {
        List<StatModifier> modifiers = new ArrayList<>();
        for (Map.Entry<StatType, Double> entry : bonusStats.entrySet()) {
            // Apply rarity multiplier to bonus stats
            double value = entry.getValue() * rarity.getStatMultiplier();
            modifiers.add(new StatModifier(
                "armor_" + id, 
                entry.getKey(), 
                value, 
                -1 // Permanent while equipped
            ));
        }
        return modifiers;
    }
    
    /**
     * Builder class for creating CustomArmorPiece instances
     */
    public static class Builder {
        private final String id;
        private final String displayName;
        private final ArmorSlot slot;
        
        private String description = "";
        private ArmorRarity rarity = ArmorRarity.COMMON;
        private int defense = 2;
        private Map<StatType, Double> bonusStats = new HashMap<>();
        private List<ArmorPassive> passives = new ArrayList<>();
        private List<String> loreLines = new ArrayList<>();
        private String setId = null;
        
        public Builder(String id, String displayName, ArmorSlot slot) {
            this.id = id;
            this.displayName = displayName;
            this.slot = slot;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder rarity(ArmorRarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        public Builder defense(int defense) {
            this.defense = defense;
            return this;
        }
        
        /**
         * Add a bonus stat to the armor piece
         */
        public Builder addStat(StatType statType, double value) {
            this.bonusStats.put(statType, value);
            return this;
        }
        
        /**
         * Add a passive effect
         */
        public Builder passive(ArmorPassive passive) {
            this.passives.add(passive);
            return this;
        }
        
        /**
         * Add a lore line to the tooltip
         */
        public Builder lore(String loreLine) {
            this.loreLines.add(loreLine);
            return this;
        }
        
        /**
         * Set the armor set this piece belongs to
         */
        public Builder setId(String setId) {
            this.setId = setId;
            return this;
        }
        
        public CustomArmorPiece build() {
            return new CustomArmorPiece(this);
        }
    }
}
