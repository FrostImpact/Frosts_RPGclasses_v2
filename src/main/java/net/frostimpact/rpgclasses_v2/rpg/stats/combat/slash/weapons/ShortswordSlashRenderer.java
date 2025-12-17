package net.frostimpact.rpgclasses_v2.rpg.stats.combat.slash.weapons;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shortsword-specific slash animations - fast, precise strikes
 */
public class ShortswordSlashRenderer {
    
    // Bright colors with white edges for crits
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    private static final Vector3f WHITE_EDGE = new Vector3f(1.0f, 1.0f, 1.0f);
    
    private static final float BASE_PARTICLE_SIZE = 0.6f;
    private static final int TOTAL_PARTICLES = 600; // Fewer particles for faster weapon
    
    /**
     * Combo 1: Quick diagonal slash from upper left to lower right
     */
    public static void renderAngledRight(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                         int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI * 0.85; // Tighter arc
            
            // Diagonal slash - upper left to lower right, more centered
            double radius = 1.8;
            double horizontalSweep = Math.sin(angle) * radius;
            double verticalDrop = Math.cos(angle) * radius * 0.6;
            double forwardReach = Math.sin(angle) * 0.4;
            
            // Center the slash more
            Vec3 centerOffset = basePos.add(0, 1.5, 0);
            
            // Add layers for thickness
            for (int layer = 0; layer < 4; layer++) {
                double layerOffset = (layer - 1.5) * 0.08;
                
                Vec3 pos = centerOffset
                    .add(right.scale(-horizontalSweep * 0.7 + layerOffset)) // Left to right
                    .add(up.scale(verticalDrop - 0.3)) // Top to bottom
                    .add(forward.scale(forwardReach));
                
                // White edges for crit effect
                Vector3f color;
                if (layer == 0 || layer == 3) {
                    color = WHITE_EDGE;
                } else if (layer == 1) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 2: Quick diagonal slash from upper right to lower left
     */
    public static void renderAngledLeft(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                        int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI * 0.85;
            
            double radius = 1.8;
            double horizontalSweep = Math.sin(angle) * radius;
            double verticalDrop = Math.cos(angle) * radius * 0.6;
            double forwardReach = Math.sin(angle) * 0.4;
            
            Vec3 centerOffset = basePos.add(0, 1.5, 0);
            
            for (int layer = 0; layer < 4; layer++) {
                double layerOffset = (layer - 1.5) * 0.08;
                
                Vec3 pos = centerOffset
                    .add(right.scale(horizontalSweep * 0.7 + layerOffset)) // Right to left
                    .add(up.scale(verticalDrop - 0.3))
                    .add(forward.scale(forwardReach));
                
                Vector3f color;
                if (layer == 0 || layer == 3) {
                    color = WHITE_EDGE;
                } else if (layer == 1) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 3: Dual X-pattern slash - two simultaneous diagonal strikes
     */
    public static void renderDualSlash(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                       int startParticle, int particleCount, float animProgress) {
        int particlesPerSlash = particleCount / 2;
        
        for (int i = 0; i < particlesPerSlash; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES / 2) break;
            
            double progress = (double) particleIndex / (TOTAL_PARTICLES / 2.0);
            double angle = progress * Math.PI * 0.9;
            
            double radius = 2.0;
            double sweep = Math.sin(angle) * radius;
            double drop = Math.cos(angle) * radius * 0.7;
            double forward_reach = Math.sin(angle) * 0.3;
            
            Vec3 centerOffset = basePos.add(0, 1.5, 0);
            
            // Add thickness with multiple layers
            for (int layer = 0; layer < 5; layer++) {
                double layerOffset = (layer - 2) * 0.1;
                
                // First diagonal: top-left to bottom-right
                Vec3 pos1 = centerOffset
                    .add(right.scale(-sweep * 0.8 + layerOffset))
                    .add(up.scale(drop - 0.4))
                    .add(forward.scale(forward_reach));
                
                // Second diagonal: top-right to bottom-left
                Vec3 pos2 = centerOffset
                    .add(right.scale(sweep * 0.8 + layerOffset))
                    .add(up.scale(drop - 0.4))
                    .add(forward.scale(forward_reach));
                
                // White edges for both slashes
                Vector3f color;
                if (layer == 0 || layer == 4) {
                    color = WHITE_EDGE;
                } else if (layer == 1 || layer == 3) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * 0.9f * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                
                level.sendParticles(dustOptions, pos1.x, pos1.y, pos1.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(dustOptions, pos2.x, pos2.y, pos2.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}