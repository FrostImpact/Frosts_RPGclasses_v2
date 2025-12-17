package net.frostimpact.rpgclasses_v2.combat.slash;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Animation handler for Claymore weapons
 * Features: Heavy, powerful slashes with improved speed and devastating AOE
 */
public class ClaymoreAnimationHandler extends WeaponAnimationHandler {
    private static final int TOTAL_PARTICLES = 800; // Larger for heavy weapon
    private static final double BASE_RADIUS = 1.8;
    private static final double MAX_RADIUS = 3.8; // Reduced from very large

    @Override
    public int getMaxComboCount() {
        return 4;
    }

    @Override
    public float getAnimationSpeedMultiplier() {
        return 1.1f; // Improved from 1.5f - now faster
    }

    @Override
    public void spawnComboParticles(ServerLevel level, Vec3 basePos, Vec3 lookDir,
                                   int comboHit, int startParticle, int particleCount,
                                   float animProgress) {
        Vec3[] dirs = getDirectionVectors(lookDir);
        Vec3 forward = dirs[0];
        Vec3 right = dirs[1];
        Vec3 up = dirs[2];

        switch (comboHit) {
            case 1 -> spawnAngledRightSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 2 -> spawnAngledLeftSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 3 -> spawnOverheadSmash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 4 -> spawnImprovedSpinAOE(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            default -> {
                // Default to first combo attack if invalid combo hit
                System.err.println("Invalid combo hit for Claymore: " + comboHit + ", defaulting to combo 1");
                spawnAngledRightSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            }
        }
    }

    /**
     * Combo 1: Heavy angled right slash
     */
    private void spawnAngledRightSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                      int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.4, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.7; // More wrap-around
            double heightRise = progress * radius * 0.35;

            // Heavy weapon has more layers and width
            for (int layer = 0; layer < 8; layer++) {
                double layerOffset = layer * 0.25;
                
                // Add horizontal thickness for wider slash
                for (int thickness = -3; thickness <= 3; thickness++) {
                    double thicknessOffset = thickness * 0.12;
                    
                    Vec3 pos = center
                            .add(right.scale(-arcSweep * 1.0 + thicknessOffset)) // Wide arc
                            .add(forward.scale(arcForward - layerOffset))
                            .add(up.scale(heightRise));

                    // Gradient: white at swing edge
                    Vector3f color = getGradientColor(progress);
                    spawnParticle(level, pos, color, alpha * 1.1f); // Slightly more visible
                }
            }
        }
    }

    /**
     * Combo 2: Heavy angled left slash
     */
    private void spawnAngledLeftSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                     int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.4, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.7; // More wrap-around
            double heightRise = (1.0 - progress) * radius * 0.35;

            // Heavy weapon has more layers and width
            for (int layer = 0; layer < 8; layer++) {
                double layerOffset = layer * 0.25;
                
                // Add horizontal thickness for wider slash
                for (int thickness = -3; thickness <= 3; thickness++) {
                    double thicknessOffset = thickness * 0.12;
                    
                    Vec3 pos = center
                            .add(right.scale(arcSweep * 1.0 + thicknessOffset)) // Wide arc
                            .add(forward.scale(arcForward - layerOffset))
                            .add(up.scale(heightRise));

                    // Gradient: white at swing edge
                    Vector3f color = getGradientColor(progress);
                    spawnParticle(level, pos, color, alpha * 1.1f);
                }
            }
        }
    }

    /**
     * Combo 3: Powerful overhead smash
     */
    private void spawnOverheadSmash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                   int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 2.5, 0); // Higher starting point
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * 0.9;
            double verticalDrop = Math.cos(angle) * radius * 0.9;
            double forwardReach = Math.sin(angle) * radius * 0.9;

            // Create powerful downward slash with more layers and thickness
            for (int layer = 0; layer < 8; layer++) {
                double layerDepth = layer * 0.2;
                
                // Add horizontal thickness
                for (int sideLayer = -3; sideLayer <= 3; sideLayer++) {
                    double sideSpread = sideLayer * 0.15;
                    
                    Vec3 pos = center
                            .add(right.scale(sideSpread))
                            .add(forward.scale(forwardReach - layerDepth))
                            .add(up.scale(-verticalDrop));

                    // Gradient: white at the leading edge (downward swing direction)
                    Vector3f color = getGradientColor(progress);
                    spawnParticle(level, pos, color, alpha * 1.1f);
                }
            }
        }
    }

    /**
     * Combo 4: Completely reworked 360-degree spin slash
     * Creates a wide, sweeping horizontal slash that rotates around the player
     */
    private void spawnImprovedSpinAOE(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                     int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.2, 0); // Waist/chest level
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;

            // Full 360 degree rotation
            double rotationAngle = progress * Math.PI * 2;
            
            // Fixed radius for consistent ring
            double slashRadius = 3.0;
            
            // Calculate base position on the circle
            double xPos = Math.cos(rotationAngle) * slashRadius;
            double zPos = Math.sin(rotationAngle) * slashRadius;
            
            // Create wide slash effect with depth layers and thickness
            for (int depthLayer = 0; depthLayer < 8; depthLayer++) {
                // Radial depth - layers going inward from outer edge
                double radiusOffset = depthLayer * 0.25;
                double layerRadius = slashRadius - radiusOffset;
                double layerX = Math.cos(rotationAngle) * layerRadius;
                double layerZ = Math.sin(rotationAngle) * layerRadius;
                
                // Add vertical thickness for more impactful appearance
                for (int vertLayer = -2; vertLayer <= 2; vertLayer++) {
                    double verticalOffset = vertLayer * 0.15;
                    
                    // Add horizontal thickness perpendicular to the slash direction
                    for (int perpLayer = -2; perpLayer <= 2; perpLayer++) {
                        double perpOffset = perpLayer * 0.1;
                        
                        // Calculate perpendicular direction to the radius
                        double perpX = -Math.sin(rotationAngle) * perpOffset;
                        double perpZ = Math.cos(rotationAngle) * perpOffset;
                        
                        Vec3 pos = center
                                .add(right.scale(layerX + perpX))
                                .add(forward.scale(layerZ + perpZ))
                                .add(up.scale(verticalOffset));

                        // Gradient: white at outer edge (swing edge), transitioning to gold at inner edge
                        double gradientProgress = (double) depthLayer / 8.0;
                        Vector3f color = getGradientColor(gradientProgress);
                        
                        spawnParticle(level, pos, color, alpha);
                    }
                }
            }
        }
    }
}
