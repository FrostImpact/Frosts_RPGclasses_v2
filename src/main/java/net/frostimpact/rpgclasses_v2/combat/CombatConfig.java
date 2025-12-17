package net.frostimpact.rpgclasses_v2.combat;

/**
 * Configuration constants for combat mechanics
 */
public class CombatConfig {
    // Base attack cooldown in ticks (20 ticks = 1 second)
    public static final int BASE_ATTACK_COOLDOWN = 10; // 0.5 seconds base
    
    // Combo system
    public static final int COMBO_RESET_TIME = 30; // 1.5 seconds of no attacks resets combo
    public static final int MAX_COMBO_COUNT = 4; // 4-hit combo before reset
    
    // Damage multipliers
    public static final double COMBO_FINISHER_MULTIPLIER = 1.2; // 4th hit deals 120% damage
    
    // Particle settings
    public static final int PARTICLES_PER_ARC = 20; // Number of particles in slash arc
    public static final double PARTICLE_SPEED = 0.05;
    
    // Pale white/brown color palette for particles
    public static final int[][] PALE_COLORS = {
        {255, 255, 250}, // Pale white 1
        {245, 245, 240}, // Pale white 2
        {235, 230, 225}, // Pale white 3
        {210, 200, 185}, // Pale brown 1
        {195, 185, 170}, // Pale brown 2
        {180, 170, 155}  // Pale brown 3
    };
}
