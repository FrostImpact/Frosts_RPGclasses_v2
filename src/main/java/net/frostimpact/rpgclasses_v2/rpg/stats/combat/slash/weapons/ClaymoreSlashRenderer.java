package net.frostimpact.rpgclasses_v2.rpg.stats.combat.slash.weapons;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Claymore-specific slash animations - heavy, wide, powerful strikes
 */
public class ClaymoreSlashRenderer {
    
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    private static final Vector3f DARK_GOLD = new Vector3f(0.9f, 0.75f, 0.25f);
    private static final Vector3f WHITE_EDGE = new Vector3f(1.0f, 1.0f, 1.0f);
    
    private static final float BASE_PARTICLE_SIZE = 0.9f; // Larger for heavy weapon
    private static final int TOTAL_PARTICLES = 1000; // More particles for impact
    
    /**
     * Combo 1: Heavy diagonal slash from left to right with rising motion
     */
    public static void renderAngledRight(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                         int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 3.0; // Wider arc
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.9;
            double heightRise = progress * radius * 0.55;
            
            Vec3 arcCenter = basePos.add(0, 1.3, 0);
            
            // Thicker slash with more layers
            for (int layer = 0; layer < 7; layer++) {
                double layerOffset = (layer - 3) * 0.15;
                
                Vec3 pos = arcCenter
                    .add(right.scale(-arcSweep * 1.2))
                    .add(forward.scale(arcForward * 1.1))
                    .add(up.scale(heightRise + layerOffset));
                
                Vector3f color;
                if (layer == 0 || layer == 6) {
                    color = WHITE_EDGE; // Bright crit edges
                } else if (layer == 1 || layer == 5) {
                    color = BRIGHT_YELLOW;
                } else if (layer == 3) {
                    color = GOLD;
                } else {
                    color = DARK_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 2: Heavy diagonal slash from right to left with rising motion
     */
    public static void renderAngledLeft(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                        int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 3.0;
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.9;
            double heightRise = (1.0 - progress) * radius * 0.55;
            
            Vec3 arcCenter = basePos.add(0, 1.3, 0);
            
            for (int layer = 0; layer < 7; layer++) {
                double layerOffset = (layer - 3) * 0.15;
                
                Vec3 pos = arcCenter
                    .add(right.scale(arcSweep * 1.2))
                    .add(forward.scale(arcForward * 1.1))
                    .add(up.scale(heightRise + layerOffset));
                
                Vector3f color;
                if (layer == 0 || layer == 6) {
                    color = WHITE_EDGE;
                } else if (layer == 1 || layer == 5) {
                    color = BRIGHT_YELLOW;
                } else if (layer == 3) {
                    color = GOLD;
                } else {
                    color = DARK_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * alpha;
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 3: Massive overhead smash downward
     */
    public static void renderOverheadSmash(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                           int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;
            
            double radius = 3.2;
            double verticalDrop = Math.cos(angle) * radius * 1.1;
            double forwardReach = Math.sin(angle) * radius * 1.2;
            double arcSweep = Math.cos(angle) * radius * 0.08;
            
            Vec3 arcCenter = basePos.add(0, 2.6, 0);
            
            // Very thick for heavy impact
            for (int layer = 0; layer < 8; layer++) {
                double layerOffset = (layer - 3.5) * 0.12;
                
                Vec3 pos = arcCenter
                    .add(right.scale(arcSweep + layerOffset))
                    .add(forward.scale(forwardReach))
                    .add(up.scale(-verticalDrop));
                
                Vector3f color;
                if (layer == 0 || layer == 7) {
                    color = WHITE_EDGE; // Bright crit edges
                } else if (layer == 1 || layer == 6) {
                    color = BRIGHT_YELLOW;
                } else if (layer == 3 || layer == 4) {
                    color = GOLD;
                } else {
                    color = DARK_GOLD;
                }
                
                float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                float size = BASE_PARTICLE_SIZE * 1.2f * alpha; // Extra large
                
                DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                level.sendParticles(dustOptions, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * Combo 4: 360-degree spin attack (AOE finisher) - thinner but wider
     */
    public static void renderSpinAOE(ServerLevel level, Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                     int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;
            
            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI * 2; // Full 360
            
            double baseRadius = 3.8; // Wide reach
            double radiusVariation = Math.sin(progress * Math.PI * 6) * 0.2;
            double radius = baseRadius + radiusVariation;
            
            double heightVariation = Math.sin(progress * Math.PI * 3) * 0.3;
            
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            
            Vec3 centerOffset = basePos.add(0, 1.0, 0);
            
            // Thinner AOE - only 3 layers instead of 4
            for (int layer = 0; layer < 3; layer++) {
                double layerRadius = radius - (layer * 0.4);
                double layerX = Math.cos(angle) * layerRadius;
                double layerZ = Math.sin(angle) * layerRadius;
                
                // Only 2 height layers for thinner vertical profile
                for (int hLayer = 0; hLayer < 2; hLayer++) {
                    double hOffset = (hLayer - 0.5) * 0.2;
                    
                    Vec3 layerPos = centerOffset
                        .add(right.scale(layerX))
                        .add(forward.scale(layerZ))
                        .add(up.scale(heightVariation + hOffset));
                    
                    Vector3f color;
                    if (layer == 0) {
                        color = WHITE_EDGE; // Outer edge is bright
                    } else if (layer == 1) {
                        color = BRIGHT_YELLOW;
                    } else {
                        color = GOLD;
                    }
                    
                    float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                    float size = BASE_PARTICLE_SIZE * 1.1f * alpha;
                    
                    DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                    level.sendParticles(dustOptions, layerPos.x, layerPos.y, layerPos.z,
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}