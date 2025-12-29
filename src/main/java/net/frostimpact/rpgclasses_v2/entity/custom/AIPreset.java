package net.frostimpact.rpgclasses_v2.entity.custom;

/**
 * Enum defining AI behavior presets for custom enemies.
 * These presets determine how the enemy will behave in combat.
 */
public enum AIPreset {
    /**
     * Aggressive - Always attacks players on sight
     * - High aggression range
     * - Pursues targets relentlessly
     * - Does not retreat
     */
    AGGRESSIVE("Aggressive", 32.0, 0.0, 1.0, true),
    
    /**
     * Defensive - Only attacks when attacked first
     * - Moderate aggression range
     * - Will retaliate when hit
     * - May retreat at low health
     */
    DEFENSIVE("Defensive", 8.0, 0.25, 0.5, false),
    
    /**
     * Passive - Does not attack unless provoked
     * - Low aggression range
     * - Only attacks when hit
     * - Likely to retreat
     */
    PASSIVE("Passive", 4.0, 0.5, 0.0, false),
    
    /**
     * Territorial - Attacks players that enter its territory
     * - Large aggression range but limited to territory
     * - Stops chasing at territory edge
     * - Defends its area
     */
    TERRITORIAL("Territorial", 16.0, 0.1, 0.8, true),
    
    /**
     * Ranged - Prefers to attack from a distance
     * - Maintains distance from target
     * - Retreats when target gets too close
     * - Uses ranged abilities
     */
    RANGED("Ranged", 24.0, 0.3, 0.6, true),
    
    /**
     * Ambush - Waits for players to get close before attacking
     * - Very short initial aggression range
     * - High damage on first strike
     * - May use stealth
     */
    AMBUSH("Ambush", 6.0, 0.2, 0.9, true),
    
    /**
     * Pack Hunter - Coordinated attacks with other pack members
     * - Calls for reinforcements
     * - Attacks in groups
     * - Flanks targets
     */
    PACK_HUNTER("Pack Hunter", 20.0, 0.15, 0.7, true),
    
    /**
     * Boss - Special AI for boss enemies
     * - Multiple attack phases
     * - Immune to knockback
     * - Uses multiple abilities
     */
    BOSS("Boss", 48.0, 0.0, 1.0, true);
    
    private final String displayName;
    private final double aggroRange;       // Range at which enemy detects players
    private final double retreatThreshold; // Health % below which enemy retreats (0-1)
    private final double aggression;       // How likely to attack vs flee (0-1)
    private final boolean initiatesAttack; // Whether enemy attacks first
    
    AIPreset(String displayName, double aggroRange, double retreatThreshold, 
             double aggression, boolean initiatesAttack) {
        this.displayName = displayName;
        this.aggroRange = aggroRange;
        this.retreatThreshold = retreatThreshold;
        this.aggression = aggression;
        this.initiatesAttack = initiatesAttack;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the aggro/detection range in blocks
     */
    public double getAggroRange() {
        return aggroRange;
    }
    
    /**
     * Get the health threshold (0-1) below which the enemy will attempt to retreat
     */
    public double getRetreatThreshold() {
        return retreatThreshold;
    }
    
    /**
     * Get the aggression level (0-1) determining attack vs flee behavior
     */
    public double getAggression() {
        return aggression;
    }
    
    /**
     * Whether this AI type initiates attacks or waits to be attacked
     */
    public boolean initiatesAttack() {
        return initiatesAttack;
    }
}
