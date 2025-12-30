package net.frostimpact.rpgclasses_v2.entity.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom enemy definition with stats, abilities, and AI behavior.
 * 
 * The AI behavior system supports the following target selection modes:
 * - NORMAL: Targets closest player/target
 * - RANGED: Keeps distance from targets
 * - RANGED_ADVANCED: Keeps distance and flees when too close
 * - FLANKER: Stays outside line of sight until in range
 * - ASSASSIN: Targets lowest HP player/target
 * - BRUTE: Targets highest HP player/target
 * - COWARD: Runs away when below 20% HP
 * - FAR_SIGHTED: Targets farthest player/target
 * - CHAOTIC: Targets random player/target
 * 
 * Targets include players and armor stands with the "player" tag.
 * 
 * Example usage:
 * <pre>
 * CustomEnemy grunt = new CustomEnemy.Builder("grunt", "Grunt")
 *     .maxHealth(40)
 *     .damage(6)
 *     .defense(2)
 *     .moveSpeed(1.0)
 *     .aiPreset(AIPreset.NORMAL)
 *     .experienceReward(10)
 *     .isBoss(false)
 *     .build();
 * </pre>
 */
public class CustomEnemy {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomEnemy.class);
    
    private final String id;
    private final String displayName;
    private final String description;
    
    // Base stats
    private final double maxHealth;
    private final double damage;
    private final double defense;
    private final double moveSpeed;
    private final double attackSpeed;
    private final double knockbackResistance;
    
    // Combat modifiers
    private final int experienceReward;
    private final boolean isBoss;
    private final int bossPhases; // Number of phases for boss fights
    
    // AI behavior
    private final AIPreset aiPreset;
    private final double customAggroRange; // Override AI preset aggro range if > 0
    
    // Abilities (normal mobs: 0-1, bosses: multiple)
    private final List<EnemyAbility> abilities;
    
    // Loot table ID (for drops)
    private final String lootTableId;
    
    // Visual properties
    private final float scale;
    private final int glowColor; // -1 for no glow
    
    // Base entity type (what vanilla mob to extend/base on)
    private final String baseEntityType;
    
    // Summon command ID
    private final String summonId;
    
    private CustomEnemy(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.maxHealth = builder.maxHealth;
        this.damage = builder.damage;
        this.defense = builder.defense;
        this.moveSpeed = builder.moveSpeed;
        this.attackSpeed = builder.attackSpeed;
        this.knockbackResistance = builder.knockbackResistance;
        this.experienceReward = builder.experienceReward;
        this.isBoss = builder.isBoss;
        this.bossPhases = builder.bossPhases;
        this.aiPreset = builder.aiPreset;
        this.customAggroRange = builder.customAggroRange;
        this.abilities = new ArrayList<>(builder.abilities);
        this.lootTableId = builder.lootTableId;
        this.scale = builder.scale;
        this.glowColor = builder.glowColor;
        this.baseEntityType = builder.baseEntityType;
        this.summonId = builder.summonId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public double getMaxHealth() { return maxHealth; }
    public double getDamage() { return damage; }
    public double getDefense() { return defense; }
    public double getMoveSpeed() { return moveSpeed; }
    public double getAttackSpeed() { return attackSpeed; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public int getExperienceReward() { return experienceReward; }
    public boolean isBoss() { return isBoss; }
    public int getBossPhases() { return bossPhases; }
    public AIPreset getAiPreset() { return aiPreset; }
    public List<EnemyAbility> getAbilities() { return new ArrayList<>(abilities); }
    public String getLootTableId() { return lootTableId; }
    public float getScale() { return scale; }
    public int getGlowColor() { return glowColor; }
    public String getBaseEntityType() { return baseEntityType; }
    public String getSummonId() { return summonId; }
    
    /**
     * Get the effective aggro range (custom if set, otherwise from AI preset)
     */
    public double getAggroRange() {
        return customAggroRange > 0 ? customAggroRange : aiPreset.getAggroRange();
    }
    
    /**
     * Check if this enemy has any abilities
     */
    public boolean hasAbilities() {
        return !abilities.isEmpty();
    }
    
    /**
     * Check if this enemy has a glow effect
     */
    public boolean hasGlow() {
        return glowColor >= 0;
    }
    
    /**
     * Get a random ability from this enemy's ability list
     */
    public EnemyAbility getRandomAbility() {
        if (abilities.isEmpty()) return null;
        int index = (int) (Math.random() * abilities.size());
        return abilities.get(index);
    }
    
    /**
     * Get the summon command for this enemy
     * @return The command to summon this enemy (without the slash)
     */
    public String getSummonCommand() {
        return "summon rpgclasses_v2:" + summonId;
    }
    
    /**
     * Builder class for creating CustomEnemy instances
     */
    public static class Builder {
        private final String id;
        private final String displayName;
        
        private String description = "";
        private double maxHealth = 20.0;
        private double damage = 3.0;
        private double defense = 0.0;
        private double moveSpeed = 1.0;
        private double attackSpeed = 1.0;
        private double knockbackResistance = 0.0;
        private int experienceReward = 5;
        private boolean isBoss = false;
        private int bossPhases = 1;
        private AIPreset aiPreset = AIPreset.NORMAL;
        private double customAggroRange = -1;
        private List<EnemyAbility> abilities = new ArrayList<>();
        private String lootTableId = "";
        private float scale = 1.0f;
        private int glowColor = -1;
        private String baseEntityType = "zombie";
        private String summonId;
        
        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
            this.summonId = id; // Default summon ID is the same as ID
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder maxHealth(double maxHealth) {
            this.maxHealth = maxHealth;
            return this;
        }
        
        public Builder damage(double damage) {
            this.damage = damage;
            return this;
        }
        
        public Builder defense(double defense) {
            this.defense = defense;
            return this;
        }
        
        public Builder moveSpeed(double moveSpeed) {
            this.moveSpeed = moveSpeed;
            return this;
        }
        
        public Builder attackSpeed(double attackSpeed) {
            this.attackSpeed = attackSpeed;
            return this;
        }
        
        public Builder knockbackResistance(double knockbackResistance) {
            this.knockbackResistance = Math.max(0, Math.min(1, knockbackResistance));
            return this;
        }
        
        public Builder experienceReward(int experienceReward) {
            this.experienceReward = experienceReward;
            return this;
        }
        
        public Builder isBoss(boolean isBoss) {
            this.isBoss = isBoss;
            if (isBoss && knockbackResistance < 0.5) {
                this.knockbackResistance = 1.0; // Bosses are knockback resistant by default
            }
            return this;
        }
        
        public Builder bossPhases(int bossPhases) {
            this.bossPhases = Math.max(1, bossPhases);
            return this;
        }
        
        public Builder aiPreset(AIPreset aiPreset) {
            this.aiPreset = aiPreset;
            return this;
        }
        
        public Builder customAggroRange(double range) {
            this.customAggroRange = range;
            return this;
        }
        
        /**
         * Add an ability to this enemy
         */
        public Builder ability(EnemyAbility ability) {
            this.abilities.add(ability);
            return this;
        }
        
        public Builder lootTableId(String lootTableId) {
            this.lootTableId = lootTableId;
            return this;
        }
        
        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }
        
        public Builder glowColor(int glowColor) {
            this.glowColor = glowColor;
            return this;
        }
        
        public Builder baseEntityType(String baseEntityType) {
            this.baseEntityType = baseEntityType;
            return this;
        }
        
        public Builder summonId(String summonId) {
            this.summonId = summonId;
            return this;
        }
        
        public CustomEnemy build() {
            // Validate: normal mobs should have 0-1 abilities
            if (!isBoss && abilities.size() > 1) {
                // Log a warning but allow it
                LOGGER.warn("Non-boss enemy '{}' has more than 1 ability", id);
            }
            return new CustomEnemy(this);
        }
    }
}
