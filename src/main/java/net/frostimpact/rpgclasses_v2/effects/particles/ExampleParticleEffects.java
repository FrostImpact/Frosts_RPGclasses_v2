package net.frostimpact.rpgclasses_v2.effects.particles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Example particle effects demonstrating the use of VectorMath and ParticleAnimator.
 * These are ready-to-use effect templates that can be customized or used as reference.
 */
public class ExampleParticleEffects {
    
    /**
     * Creates a magical circle effect on the ground
     * @param level Server level
     * @param center Center position
     * @param radius Circle radius
     */
    public static void magicCircle(ServerLevel level, Vec3 center, double radius) {
        // Outer ring
        ParticleAnimator.spawnCircle(level, ParticleTypes.ENCHANT, center, radius, 32, 0.0);
        
        // Inner ring
        ParticleAnimator.spawnCircle(level, ParticleTypes.ENCHANT, center, radius * 0.5, 16, 0.0);
        
        // Center burst
        ParticleAnimator.spawnCircle(level, ParticleTypes.GLOW, center.add(0, 0.1, 0), 0.5, 8, 0.05);
    }
    
    /**
     * Creates a rising spiral effect
     * @param level Server level
     * @param center Base position
     */
    public static void risingSpiral(ServerLevel level, Vec3 center) {
        ParticleAnimator.spawnSpiral(level, ParticleTypes.SOUL_FIRE_FLAME, center, 
                                    0.5, 0.1, 3.0, 50, 3.0);
    }
    
    /**
     * Creates an explosion sphere effect
     * @param level Server level
     * @param center Explosion center
     * @param radius Explosion radius
     */
    public static void explosionSphere(ServerLevel level, Vec3 center, double radius) {
        ParticleAnimator.spawnSphere(level, ParticleTypes.FLAME, center, radius, 100, true);
        ParticleAnimator.spawnSphere(level, ParticleTypes.SMOKE, center, radius * 1.2, 50, true);
    }
    
    /**
     * Creates a wave of energy particles
     * @param level Server level
     * @param start Start position
     * @param direction Direction to send the wave
     * @param length Wave length
     */
    public static void energyWave(ServerLevel level, Vec3 start, Vec3 direction, double length) {
        ParticleAnimator.spawnWave(level, ParticleTypes.ELECTRIC_SPARK, start, direction,
                                  length, 0.5, 2.0, 50);
    }
    
    /**
     * Creates a teleport effect with particles swirling into a point
     * @param level Server level
     * @param center Teleport destination
     */
    public static void teleportEffect(ServerLevel level, Vec3 center) {
        // Upward spiral arriving at destination
        ParticleAnimator.spawnSpiral(level, ParticleTypes.PORTAL, center.add(0, -2, 0),
                                    2.0, 0.0, 2.0, 80, 5.0);
        
        // Burst at destination
        ParticleAnimator.spawnSphere(level, ParticleTypes.REVERSE_PORTAL, center, 0.5, 30, true);
    }
    
    /**
     * Creates a healing aura effect
     * @param level Server level
     * @param center Player position
     */
    public static void healingAura(ServerLevel level, Vec3 center) {
        double time = level.getGameTime() * 0.1;
        Vec3[] circlePoints = VectorMath.createCircle(center, 1.5, 12, time);
        
        for (Vec3 point : circlePoints) {
            Vec3 upward = new Vec3(0, 0.05, 0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                              point.x, point.y, point.z, 0,
                              upward.x, upward.y, upward.z, 1.0);
        }
    }
    
    /**
     * Creates a slash arc effect (useful for melee attacks)
     * @param level Server level
     * @param start Start position
     * @param lookVec Player's look vector
     * @param rightVec Vector to the player's right
     */
    public static void slashArc(ServerLevel level, Vec3 start, Vec3 lookVec, Vec3 rightVec) {
        // Create arc using bezier curve
        Vec3 p0 = start.add(rightVec.scale(-1.0));
        Vec3 p1 = start.add(lookVec.scale(1.5)).add(rightVec.scale(-0.5));
        Vec3 p2 = start.add(lookVec.scale(1.5)).add(rightVec.scale(0.5));
        Vec3 p3 = start.add(rightVec.scale(1.0));
        
        ParticleAnimator.spawnBezierCurve(level, ParticleTypes.SWEEP_ATTACK, p0, p1, p2, p3, 20);
    }
    
    /**
     * Creates a protective barrier effect around a player
     * @param level Server level
     * @param center Player position
     * @param radius Barrier radius
     */
    public static void protectiveBarrier(ServerLevel level, Vec3 center, double radius) {
        // Horizontal ring at chest height
        ParticleAnimator.spawnCircle(level, ParticleTypes.END_ROD, 
                                    center.add(0, 1.0, 0), radius, 24, 0.0);
        
        // Vertical particles
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i / 8);
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            for (int j = 0; j < 5; j++) {
                double y = center.y + j * 0.4;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 0, 0, 0, 0, 0);
            }
        }
    }
}
