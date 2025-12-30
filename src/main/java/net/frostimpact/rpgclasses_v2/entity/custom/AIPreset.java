package net.frostimpact.rpgclasses_v2.entity.custom;

/**
 * Enum defining AI behavior presets for custom enemies.
 * These presets determine how the enemy will behave in combat and target selection.
 */
public enum AIPreset {
    /**
     * Normal - Basic enemy behavior
     * - Targets the closest player
     * - Standard combat behavior
     */
    NORMAL("Normal", "Targets the closest player", 24.0, 0.0, true, false, false),
    
    /**
     * Ranged - Prefers to attack from a distance
     * - Will attempt to keep a distance between itself and the player
     * - Uses ranged attacks
     */
    RANGED("Ranged", "Keeps distance from players", 32.0, 0.0, true, true, false),
    
    /**
     * Ranged (Advanced) - Sophisticated ranged combat
     * - Will attempt to keep a distance between itself and the player
     * - Will run away if a player is too close
     */
    RANGED_ADVANCED("Ranged (Advanced)", "Keeps distance, flees when too close", 32.0, 0.0, true, true, true),
    
    /**
     * Flanker - Tactical positioning
     * - Will attempt to stay outside the player's line of sight if not in range
     * - Approaches from blind spots
     */
    FLANKER("Flanker", "Stays outside line of sight until in range", 20.0, 0.0, true, false, false),
    
    /**
     * Assassin - Targets weak prey
     * - Will target the lowest HP player in range
     * - Focuses on finishing wounded targets
     */
    ASSASSIN("Assassin", "Targets the lowest HP player", 24.0, 0.0, true, false, false),
    
    /**
     * Brute - Targets strong prey
     * - Will target the highest HP player in range
     * - Seeks out the strongest opponent
     */
    BRUTE("Brute", "Targets the highest HP player", 24.0, 0.0, true, false, false),
    
    /**
     * Coward - Self-preservation instinct
     * - Will run away when below 20% HP
     * - Normal behavior otherwise
     */
    COWARD("Coward", "Runs away when below 20% HP", 24.0, 0.2, true, false, false),
    
    /**
     * Far Sighted - Long-range targeter
     * - Targets the farthest player in range
     * - Prefers distant engagements
     */
    FAR_SIGHTED("Far Sighted", "Targets the farthest player", 40.0, 0.0, true, false, false),
    
    /**
     * Chaotic - Unpredictable behavior
     * - Targets a random player in range
     * - Switches targets frequently
     */
    CHAOTIC("Chaotic", "Targets a random player", 24.0, 0.0, true, false, false);
    
    private final String displayName;
    private final String description;
    private final double aggroRange;       // Range at which enemy detects players/targets
    private final double retreatThreshold; // Health % below which enemy retreats (0-1)
    private final boolean initiatesAttack; // Whether enemy attacks first
    private final boolean prefersDistance; // Whether enemy tries to keep distance
    private final boolean fleesWhenClose;  // Whether enemy runs when target is too close
    
    AIPreset(String displayName, String description, double aggroRange, double retreatThreshold, 
             boolean initiatesAttack, boolean prefersDistance, boolean fleesWhenClose) {
        this.displayName = displayName;
        this.description = description;
        this.aggroRange = aggroRange;
        this.retreatThreshold = retreatThreshold;
        this.initiatesAttack = initiatesAttack;
        this.prefersDistance = prefersDistance;
        this.fleesWhenClose = fleesWhenClose;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
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
     * Whether this AI type initiates attacks or waits to be attacked
     */
    public boolean initiatesAttack() {
        return initiatesAttack;
    }
    
    /**
     * Whether this AI type prefers to maintain distance from targets
     */
    public boolean prefersDistance() {
        return prefersDistance;
    }
    
    /**
     * Whether this AI type will flee when a target gets too close
     */
    public boolean fleesWhenClose() {
        return fleesWhenClose;
    }
    
    /**
     * Get the preferred combat distance for ranged types
     * @return Preferred distance in blocks, or 0 for melee types
     */
    public double getPreferredDistance() {
        if (prefersDistance) {
            return 12.0; // Preferred distance for ranged enemies
        }
        return 0.0;
    }
    
    /**
     * Get the "too close" threshold that triggers fleeing for RANGED_ADVANCED
     * @return Distance in blocks below which the enemy will flee
     */
    public double getFleeDistance() {
        if (fleesWhenClose) {
            return 5.0; // Distance at which ranged advanced flees
        }
        return 0.0;
    }
}
