package net.frostimpact.rpgclasses_v2.rpg.stats.combat.slash.weapons;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Longsword-specific slash animations - balanced, elegant strikes
 */
public class LongswordSlashRenderer {
    
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    private static final Vector3f LIGHT_GOLD = new Vector3f(1.0f, 0.90f, 0.45f);
    private static final Vector3f WHITE_EDGE = new Vector3f(1.0f, 1.0f, 1.0f);
    
    private static final float BASE_PARTICLE_SIZE = 0.7f;
    private static final int TOTAL_PARTICLES = 800;
    
    /**
     * Combo 1: Rising slash from left to right, angled upward
     */
    public static void renderRaisedRight(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                         int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 2.5;
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.8;
            double heightRise = progress * radius * 0.5;
            
            Vec3 arcCenter = basePos.add(0, 1.2, 0);
            
            // Multiple layers for volume
            for (int layer = 0; layer < 5; layer++) {
                double layerOffset = (layer - 2) * 0.12;
                
                Vec3 pos = arcCenter
                    .add(right.scale(arcSweep))
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise + layerOffset));
                
                Vector3f color;
                if (layer == 0 || layer == 4) {
                    color = WHITE_EDGE; // Crit edges
                } else if (layer == 2) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = LIGHT_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 2: Rising slash from right to left, angled upward
     */
    public static void renderRaisedLeft(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                        int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 2.5;
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.8;
            double heightRise = (1.0 - progress) * radius * 0.5;
            
            Vec3 arcCenter = basePos.add(0, 1.2, 0);
            
            for (int layer = 0; layer < 5; layer++) {
                double layerOffset = (layer - 2) * 0.12;
                
                Vec3 pos = arcCenter
                    .add(right.scale(-arcSweep))
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise + layerOffset));
                
                Vector3f color;
                if (layer == 0 || layer == 4) {
                    color = WHITE_EDGE;
                } else if (layer == 2) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = LIGHT_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 3: Perfectly horizontal slash from left to right
     */
    public static void renderFlat(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                   int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 2.5;
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius;
            
            Vec3 arcCenter = basePos.add(0, 1.4, 0);
            
            for (int layer = 0; layer < 5; layer++) {
                double layerOffset = (layer - 2) * 0.12;
                
                Vec3 pos = arcCenter
                    .add(right.scale(-arcSweep))
                    .add(forward.scale(arcForward))
                    .add(up.scale(layerOffset)); // Pure horizontal
                
                Vector3f color;
                if (layer == 0 || layer == 4) {
                    color = WHITE_EDGE;
                } else if (layer == 2) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = LIGHT_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 4: Overhead vertical slash downward (finisher)
     */
    public static void renderOverhead(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                      int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 2.8;
            double verticalDrop = Math.cos(angle) * radius * 0.9;
            double forwardReach = Math.sin(angle) * radius;
            double arcSweep = Math.cos(angle) * radius * 0.15;
            
            Vec3 arcCenter = basePos.add(0, 2.4, 0);
            
            for (int layer = 0; layer < 6; layer++) {
                double layerOffset = (layer - 2.5) * 0.1;
                
                Vec3 pos = arcCenter
                    .add(right.scale(arcSweep + layerOffset))
                    .add(forward.scale(forwardReach))
                    .add(up.scale(-verticalDrop));
                
                Vector3f color;
                if (layer == 0 || layer == 5) {
                    color = WHITE_EDGE; // Bright crit edges
                } else if (layer == 2 || layer == 3) {
                    color = BRIGHT_YELLOW;
                } else {
                    color = GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * 1.1f * alpha; // Slightly larger for impact
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}