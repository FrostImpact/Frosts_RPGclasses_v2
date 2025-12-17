package net.frostimpact.rpgclasses_v2.effects.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class for spawning particle effects using vector math.
 * Provides easy-to-use methods for creating common particle patterns.
 */
public class ParticleAnimator {
    
    /**
     * Spawn particles in a circle pattern
     * @param level Server level to spawn particles in
     * @param particle Particle type to spawn
     * @param center Center position
     * @param radius Circle radius
     * @param particleCount Number of particles
     * @param speed Particle speed
     */
    public static void spawnCircle(ServerLevel level, ParticleOptions particle, Vec3 center, 
                                   double radius, int particleCount, double speed) {
        Vec3[] points = VectorMath.createCircle(center, radius, particleCount, 0);
        for (Vec3 point : points) {
            Vec3 velocity = point.subtract(center).normalize().scale(speed);
            level.sendParticles(particle, point.x, point.y, point.z, 0, 
                              velocity.x, velocity.y, velocity.z, 1.0);
        }
    }
    
    /**
     * Spawn particles in a spiral pattern
     * @param level Server level to spawn particles in
     * @param particle Particle type to spawn
     * @param center Center position
     * @param startRadius Starting radius
     * @param endRadius Ending radius
     * @param height Total height
     * @param particleCount Number of particles
     * @param rotations Number of spiral rotations
     */
    public static void spawnSpiral(ServerLevel level, ParticleOptions particle, Vec3 center,
                                   double startRadius, double endRadius, double height,
                                   int particleCount, double rotations) {
        Vec3[] points = VectorMath.createSpiral(center, startRadius, endRadius, height, 
                                               particleCount, rotations);
        for (Vec3 point : points) {
            level.sendParticles(particle, point.x, point.y, point.z, 0, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn particles in a sphere pattern
     * @param level Server level to spawn particles in
     * @param particle Particle type to spawn
     * @param center Center position
     * @param radius Sphere radius
     * @param particleCount Number of particles
     * @param explode If true, particles move outward; if false, particles are static
     */
    public static void spawnSphere(ServerLevel level, ParticleOptions particle, Vec3 center,
                                   double radius, int particleCount, boolean explode) {
        Vec3[] points = VectorMath.createSphere(center, radius, particleCount);
        for (Vec3 point : points) {
            if (explode) {
                Vec3 velocity = point.subtract(center).normalize().scale(0.1);
                level.sendParticles(particle, point.x, point.y, point.z, 0,
                                  velocity.x, velocity.y, velocity.z, 1.0);
            } else {
                level.sendParticles(particle, point.x, point.y, point.z, 0, 0, 0, 0, 0);
            }
        }
    }
    
    /**
     * Spawn particles along a wave pattern
     * @param level Server level to spawn particles in
     * @param particle Particle type to spawn
     * @param start Start position
     * @param direction Direction vector
     * @param length Total wave length
     * @param amplitude Wave amplitude
     * @param frequency Wave frequency
     * @param particleCount Number of particles
     */
    public static void spawnWave(ServerLevel level, ParticleOptions particle, Vec3 start,
                                 Vec3 direction, double length, double amplitude, 
                                 double frequency, int particleCount) {
        Vec3[] points = VectorMath.createWave(start, direction, length, amplitude, 
                                             frequency, particleCount);
        for (Vec3 point : points) {
            level.sendParticles(particle, point.x, point.y, point.z, 0, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn particles along a bezier curve
     * @param level Server level to spawn particles in
     * @param particle Particle type to spawn
     * @param p0 Start point
     * @param p1 First control point
     * @param p2 Second control point
     * @param p3 End point
     * @param particleCount Number of particles
     */
    public static void spawnBezierCurve(ServerLevel level, ParticleOptions particle,
                                        Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            double t = (double) i / (particleCount - 1);
            Vec3 point = VectorMath.bezier(p0, p1, p2, p3, t);
            level.sendParticles(particle, point.x, point.y, point.z, 0, 0, 0, 0, 0);
        }
    }
}
