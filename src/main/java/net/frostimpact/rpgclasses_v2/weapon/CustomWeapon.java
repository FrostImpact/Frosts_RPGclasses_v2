package net.frostimpact.rpgclasses_v2.weapon;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom weapon with stats, abilities, passives, and rarity.
 * This is the core class for the custom weapon system.
 * 
 * Example usage:
 * <pre>
 * CustomWeapon flameSlayer = new CustomWeapon.Builder("flame_slayer", "Flame Slayer", WeaponType.LONGSWORD)
 *     .rarity(WeaponRarity.EPIC)
 *     .baseDamage(12)
 *     .attackSpeed(1.4)
 *     .criticalChance(15)
 *     .addStat(StatType.DAMAGE, 5)
 *     .addStat(StatType.ATTACK_SPEED, 10)
 *     .ability(new FireBlastAbility())
 *     .passive(new BurningStrikePassive())
 *     .description("A legendary sword forged in dragon fire")
 *     .build();
 * </pre>
 */
public class CustomWeapon {
    private final String id;
    private final String displayName;
    private final String description;
    private final WeaponType weaponType;
    private final WeaponRarity rarity;
    
    // Base combat stats
    private final int baseDamage;
    private final double attackSpeed;
    private final int criticalChance;
    private final int criticalDamage;
    private final int reach;
    
    // Bonus stat modifiers granted when holding the weapon
    private final Map<StatType, Double> bonusStats;
    
    // Ability triggered on right-click
    private final WeaponAbility ability;
    
    // Passive effects
    private final List<WeaponPassive> passives;
    
    // Lore lines for item tooltip
    private final List<String> loreLines;
    
    private CustomWeapon(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.weaponType = builder.weaponType;
        this.rarity = builder.rarity;
        this.baseDamage = builder.baseDamage;
        this.attackSpeed = builder.attackSpeed;
        this.criticalChance = builder.criticalChance;
        this.criticalDamage = builder.criticalDamage;
        this.reach = builder.reach;
        this.bonusStats = new HashMap<>(builder.bonusStats);
        this.ability = builder.ability;
        this.passives = new ArrayList<>(builder.passives);
        this.loreLines = new ArrayList<>(builder.loreLines);
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public WeaponType getWeaponType() { return weaponType; }
    public WeaponRarity getRarity() { return rarity; }
    
    /**
     * Get the effective base damage (including rarity multiplier)
     */
    public int getEffectiveDamage() {
        return (int) (baseDamage * rarity.getStatMultiplier());
    }
    
    /**
     * Get the raw base damage without rarity multiplier
     */
    public int getRawBaseDamage() { return baseDamage; }
    
    public double getAttackSpeed() { return attackSpeed; }
    public int getCriticalChance() { return criticalChance; }
    public int getCriticalDamage() { return criticalDamage; }
    public int getReach() { return reach; }
    public Map<StatType, Double> getBonusStats() { return new HashMap<>(bonusStats); }
    public WeaponAbility getAbility() { return ability; }
    public List<WeaponPassive> getPassives() { return new ArrayList<>(passives); }
    public List<String> getLoreLines() { return new ArrayList<>(loreLines); }
    
    /**
     * Check if this weapon has a right-click ability
     */
    public boolean hasAbility() {
        return ability != null;
    }
    
    /**
     * Check if this weapon has any passives
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
     * Create stat modifiers for all bonus stats on this weapon
     */
    public List<StatModifier> createStatModifiers() {
        List<StatModifier> modifiers = new ArrayList<>();
        for (Map.Entry<StatType, Double> entry : bonusStats.entrySet()) {
            // Apply rarity multiplier to bonus stats
            double value = entry.getValue() * rarity.getStatMultiplier();
            modifiers.add(new StatModifier(
                "weapon_" + id, 
                entry.getKey(), 
                value, 
                -1 // Permanent while equipped
            ));
        }
        return modifiers;
    }
    
    /**
     * Builder class for creating CustomWeapon instances
     */
    public static class Builder {
        private final String id;
        private final String displayName;
        private final WeaponType weaponType;
        
        private String description = "";
        private WeaponRarity rarity = WeaponRarity.COMMON;
        private int baseDamage = 5;
        private double attackSpeed = 1.0;
        private int criticalChance = 5;
        private int criticalDamage = 150;
        private int reach = 3;
        private Map<StatType, Double> bonusStats = new HashMap<>();
        private WeaponAbility ability = null;
        private List<WeaponPassive> passives = new ArrayList<>();
        private List<String> loreLines = new ArrayList<>();
        
        public Builder(String id, String displayName, WeaponType weaponType) {
            this.id = id;
            this.displayName = displayName;
            this.weaponType = weaponType;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder rarity(WeaponRarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        public Builder baseDamage(int baseDamage) {
            this.baseDamage = baseDamage;
            return this;
        }
        
        public Builder attackSpeed(double attackSpeed) {
            this.attackSpeed = attackSpeed;
            return this;
        }
        
        public Builder criticalChance(int criticalChance) {
            this.criticalChance = criticalChance;
            return this;
        }
        
        public Builder criticalDamage(int criticalDamage) {
            this.criticalDamage = criticalDamage;
            return this;
        }
        
        public Builder reach(int reach) {
            this.reach = reach;
            return this;
        }
        
        /**
         * Add a bonus stat to the weapon
         */
        public Builder addStat(StatType statType, double value) {
            this.bonusStats.put(statType, value);
            return this;
        }
        
        /**
         * Set the right-click ability
         */
        public Builder ability(WeaponAbility ability) {
            this.ability = ability;
            return this;
        }
        
        /**
         * Add a passive effect
         */
        public Builder passive(WeaponPassive passive) {
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
        
        public CustomWeapon build() {
            return new CustomWeapon(this);
        }
    }
}
