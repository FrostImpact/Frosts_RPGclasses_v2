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

            // Heavy weapon has more layers
            for (int layer = 0; layer < 4; layer++) {
                double layerOffset = layer * 0.25;
                
                Vec3 pos = center
                        .add(right.scale(-arcSweep * 1.0)) // Wide arc
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 4, layer == 0);
                spawnParticle(level, pos, color, alpha * 1.1f); // Slightly more visible
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

            // Heavy weapon has more layers
            for (int layer = 0; layer < 4; layer++) {
                double layerOffset = layer * 0.25;
                
                Vec3 pos = center
                        .add(right.scale(arcSweep * 1.0)) // Wide arc
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 4, layer == 0);
                spawnParticle(level, pos, color, alpha * 1.1f);
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

            // Create powerful downward slash with side layers
            for (int layer = 0; layer < 4; layer++) {
                double sideSpread = (layer - 1.5) * 0.2;
                
                Vec3 pos = center
                        .add(right.scale(sideSpread))
                        .add(forward.scale(forwardReach))
                        .add(up.scale(-verticalDrop));

                Vector3f color = selectColor(layer, 4, layer == 0 || layer == 3);
                spawnParticle(level, pos, color, alpha * 1.1f);
            }
        }
    }

    /**
     * Combo 4: Improved 360-degree spin AOE with better visual impact
     */
    private void spawnImprovedSpinAOE(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                     int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.0, 0); // Waist level
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;

            // Full 360 with dynamic expansion
            double angle = progress * Math.PI * 2;
            
            // Dynamic radius that expands outward (smaller but more impactful)
            double expansionFactor = Math.min(1.0, easedProgress * 1.5);
            double baseRadius = 2.8 * expansionFactor; // Reduced from 3.5
            double radiusWave = Math.sin(progress * Math.PI * 6) * 0.25;
            double radius = baseRadius + radiusWave;

            // Height wave for more dynamic movement
            double heightWave = Math.sin(progress * Math.PI * 3) * 0.35;

            // Calculate position
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;

            // Multiple radius layers for thick, powerful effect
            for (int rLayer = 0; rLayer < 5; rLayer++) {
                double layerRadius = radius - (rLayer * 0.25);
                double layerX = Math.cos(angle) * layerRadius;
                double layerZ = Math.sin(angle) * layerRadius;

                // Height layers for vertical thickness
                for (int hLayer = 0; hLayer < 3; hLayer++) {
                    double hOffset = (hLayer - 1) * 0.3;

                    Vec3 pos = center
                            .add(right.scale(layerX))
                            .add(forward.scale(layerZ))
                            .add(up.scale(heightWave + hOffset));

                    // Outer layers brightest for impact
                    Vector3f color;
                    if (rLayer == 0) {
                        color = BRIGHT_YELLOW;
                    } else if (rLayer <= 1) {
                        color = LIGHT_GOLD;
                    } else {
                        color = GOLD;
                    }

                    // Larger particles for devastating AOE feel
                    float particleAlpha = alpha * 1.2f;
                    spawnParticle(level, pos, color, particleAlpha);
                }
            }
        }
    }
}
