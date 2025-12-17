package net.frostimpact.rpgclasses_v2.rpg.stats;

public enum StatType {
    // Integer-based additive stats
    MAX_HEALTH(false),
    HEALTH_REGEN(false),
    MAX_MANA(false),
    MANA_REGEN(false),
    DAMAGE(false),
    DEFENSE(false),
    COOLDOWN_REDUCTION(false),
    
    // Percentage-based stats
    ATTACK_SPEED(true),
    MOVE_SPEED(true);
    
    private final boolean isPercentage;
    
    StatType(boolean isPercentage) {
        this.isPercentage = isPercentage;
    }
    
    /**
     * Returns true if this stat is percentage-based (like speed stats),
     * false if it's an integer additive stat (like damage, health)
     */
    public boolean isPercentage() {
        return isPercentage;
    }
}
