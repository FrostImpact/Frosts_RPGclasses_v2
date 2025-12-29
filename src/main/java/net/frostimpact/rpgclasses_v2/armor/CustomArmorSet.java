package net.frostimpact.rpgclasses_v2.armor;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete armor set with set bonuses.
 * A set consists of 4 armor pieces (helmet, chestplate, leggings, boots).
 * 
 * Example usage:
 * <pre>
 * CustomArmorSet shadowSet = new CustomArmorSet.Builder("shadow_set", "Shadow Set")
 *     .rarity(ArmorRarity.EPIC)
 *     .helmet(shadowHelmet)
 *     .chestplate(shadowChestplate)
 *     .leggings(shadowLeggings)
 *     .boots(shadowBoots)
 *     .setBonus2pc(new ShadowCloak2pcBonus())
 *     .setBonus4pc(new ShadowCloak4pcBonus())
 *     .build();
 * </pre>
 */
public class CustomArmorSet {
    private final String id;
    private final String displayName;
    private final String description;
    private final ArmorRarity rarity;
    
    // The four armor pieces
    private final CustomArmorPiece helmet;
    private final CustomArmorPiece chestplate;
    private final CustomArmorPiece leggings;
    private final CustomArmorPiece boots;
    
    // Set bonuses (activated when wearing multiple pieces)
    private final ArmorPassive setBonus2pc; // 2-piece bonus
    private final ArmorPassive setBonus4pc; // 4-piece (full set) bonus
    
    // Bonus stats granted by the full set
    private final Map<StatType, Double> fullSetBonusStats;
    
    private CustomArmorSet(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.rarity = builder.rarity;
        this.helmet = builder.helmet;
        this.chestplate = builder.chestplate;
        this.leggings = builder.leggings;
        this.boots = builder.boots;
        this.setBonus2pc = builder.setBonus2pc;
        this.setBonus4pc = builder.setBonus4pc;
        this.fullSetBonusStats = new HashMap<>(builder.fullSetBonusStats);
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ArmorRarity getRarity() { return rarity; }
    public CustomArmorPiece getHelmet() { return helmet; }
    public CustomArmorPiece getChestplate() { return chestplate; }
    public CustomArmorPiece getLeggings() { return leggings; }
    public CustomArmorPiece getBoots() { return boots; }
    public ArmorPassive getSetBonus2pc() { return setBonus2pc; }
    public ArmorPassive getSetBonus4pc() { return setBonus4pc; }
    public Map<StatType, Double> getFullSetBonusStats() { return new HashMap<>(fullSetBonusStats); }
    
    /**
     * Get all pieces in this set
     */
    public List<CustomArmorPiece> getAllPieces() {
        List<CustomArmorPiece> pieces = new ArrayList<>();
        if (helmet != null) pieces.add(helmet);
        if (chestplate != null) pieces.add(chestplate);
        if (leggings != null) pieces.add(leggings);
        if (boots != null) pieces.add(boots);
        return pieces;
    }
    
    /**
     * Get an armor piece by slot
     */
    public CustomArmorPiece getPiece(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> helmet;
            case CHESTPLATE -> chestplate;
            case LEGGINGS -> leggings;
            case BOOTS -> boots;
        };
    }
    
    /**
     * Check if this set has a 2-piece bonus
     */
    public boolean has2pcBonus() {
        return setBonus2pc != null;
    }
    
    /**
     * Check if this set has a 4-piece (full set) bonus
     */
    public boolean has4pcBonus() {
        return setBonus4pc != null;
    }
    
    /**
     * Create stat modifiers for the full set bonus stats
     */
    public List<StatModifier> createFullSetStatModifiers() {
        List<StatModifier> modifiers = new ArrayList<>();
        for (Map.Entry<StatType, Double> entry : fullSetBonusStats.entrySet()) {
            double value = entry.getValue() * rarity.getStatMultiplier();
            modifiers.add(new StatModifier(
                "armor_set_" + id, 
                entry.getKey(), 
                value, 
                -1
            ));
        }
        return modifiers;
    }
    
    /**
     * Builder class for creating CustomArmorSet instances
     */
    public static class Builder {
        private final String id;
        private final String displayName;
        
        private String description = "";
        private ArmorRarity rarity = ArmorRarity.COMMON;
        private CustomArmorPiece helmet;
        private CustomArmorPiece chestplate;
        private CustomArmorPiece leggings;
        private CustomArmorPiece boots;
        private ArmorPassive setBonus2pc;
        private ArmorPassive setBonus4pc;
        private Map<StatType, Double> fullSetBonusStats = new HashMap<>();
        
        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder rarity(ArmorRarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        public Builder helmet(CustomArmorPiece helmet) {
            this.helmet = helmet;
            return this;
        }
        
        public Builder chestplate(CustomArmorPiece chestplate) {
            this.chestplate = chestplate;
            return this;
        }
        
        public Builder leggings(CustomArmorPiece leggings) {
            this.leggings = leggings;
            return this;
        }
        
        public Builder boots(CustomArmorPiece boots) {
            this.boots = boots;
            return this;
        }
        
        /**
         * Set the 2-piece set bonus
         */
        public Builder setBonus2pc(ArmorPassive bonus) {
            this.setBonus2pc = bonus;
            return this;
        }
        
        /**
         * Set the 4-piece (full set) bonus
         */
        public Builder setBonus4pc(ArmorPassive bonus) {
            this.setBonus4pc = bonus;
            return this;
        }
        
        /**
         * Add a bonus stat granted when wearing the full set
         */
        public Builder addFullSetStat(StatType statType, double value) {
            this.fullSetBonusStats.put(statType, value);
            return this;
        }
        
        public CustomArmorSet build() {
            return new CustomArmorSet(this);
        }
    }
}
