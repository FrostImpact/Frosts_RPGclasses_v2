package net.frostimpact.rpgclasses_v2.rpg.stats.weapon;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines stat bonuses that a weapon provides when equipped
 */
public class WeaponStats {
    private final Map<StatType, Double> statBonuses;
    
    private WeaponStats(Builder builder) {
        this.statBonuses = new HashMap<>(builder.statBonuses);
    }
    
    public double getStatBonus(StatType statType) {
        return statBonuses.getOrDefault(statType, 0.0);
    }
    
    public Map<StatType, Double> getAllBonuses() {
        return new HashMap<>(statBonuses);
    }
    
    public boolean hasStat(StatType statType) {
        return statBonuses.containsKey(statType);
    }
    
    /**
     * Builder for creating weapon stats
     */
    public static class Builder {
        private final Map<StatType, Double> statBonuses = new HashMap<>();
        
        public Builder damage(double value) {
            statBonuses.put(StatType.DAMAGE, value);
            return this;
        }
        
        public Builder attackSpeed(double value) {
            statBonuses.put(StatType.ATTACK_SPEED, value);
            return this;
        }
        
        public Builder defense(double value) {
            statBonuses.put(StatType.DEFENSE, value);
            return this;
        }
        
        public Builder moveSpeed(double value) {
            statBonuses.put(StatType.MOVE_SPEED, value);
            return this;
        }
        
        public Builder maxHealth(double value) {
            statBonuses.put(StatType.MAX_HEALTH, value);
            return this;
        }
        
        public Builder maxMana(double value) {
            statBonuses.put(StatType.MAX_MANA, value);
            return this;
        }
        
        public Builder healthRegen(double value) {
            statBonuses.put(StatType.HEALTH_REGEN, value);
            return this;
        }
        
        public Builder manaRegen(double value) {
            statBonuses.put(StatType.MANA_REGEN, value);
            return this;
        }
        
        public Builder cooldownReduction(double value) {
            statBonuses.put(StatType.COOLDOWN_REDUCTION, value);
            return this;
        }
        
        /**
         * Set any stat type with a value
         */
        public Builder stat(StatType statType, double value) {
            statBonuses.put(statType, value);
            return this;
        }
        
        public WeaponStats build() {
            return new WeaponStats(this);
        }
    }
    
    /**
     * Create a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Empty weapon stats (no bonuses)
     */
    public static WeaponStats empty() {
        return new Builder().build();
    }
}