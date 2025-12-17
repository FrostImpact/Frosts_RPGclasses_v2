package net.frostimpact.rpgclasses_v2.combat.slash;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Animation handler for Shortsword weapons
 * Features: Fast, centered slashes with X-pattern combo and lunge finisher
 */
public class ShortswordAnimationHandler extends WeaponAnimationHandler {
    private static final int TOTAL_PARTICLES = 600; // Smaller for shortsword
    private static final double BASE_RADIUS = 1.2; // Smaller radius
    private static final double MAX_RADIUS = 2.5; // Reduced max radius

    @Override
    public int getMaxComboCount() {
        return 4; // Updated to 4-hit combo
    }

    @Override
    public float getAnimationSpeedMultiplier() {
        return 0.6f; // Fast animations
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
            case 1 -> spawnCenteredRightSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 2 -> spawnCenteredLeftSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 3 -> spawnXSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 4 -> spawnLungeSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
        }
    }

    /**
     * Combo 1: Centered right slash - sweeps from left to right through center
     */
    private void spawnCenteredRightSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                        int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0); // Chest level, centered
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI; // 180 degree arc

            // Centered arc - extends equally around player
            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.6; // More wrap-around
            double heightRise = progress * radius * 0.25; // Slight upward rise

            // Create layered effect
            for (int layer = 0; layer < 3; layer++) {
                double layerOffset = layer * 0.2;
                
                Vec3 pos = center
                        .add(right.scale(-arcSweep * 0.9)) // Left to right (centered)
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }

    /**
     * Combo 2: Centered left slash - sweeps from right to left through center
     */
    private void spawnCenteredLeftSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                       int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0); // Chest level, centered
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI; // 180 degree arc

            // Centered arc - extends equally around player
            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.6; // More wrap-around
            double heightRise = (1.0 - progress) * radius * 0.25; // Slight upward rise

            // Create layered effect
            for (int layer = 0; layer < 3; layer++) {
                double layerOffset = layer * 0.2;
                
                Vec3 pos = center
                        .add(right.scale(arcSweep * 0.9)) // Right to left (centered)
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }

    /**
     * Combo 3: X-pattern dual slash in front of player
     */
    private void spawnXSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                            int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(forward.scale(0.8)).add(0, 1.3, 0); // Positioned in front
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        int particlesPerSlash = particleCount / 2;

        for (int i = 0; i < particlesPerSlash; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES / 2) break;

            double progress = (double) particleIndex / (TOTAL_PARTICLES / 2);
            double angle = progress * Math.PI; // 180 degree for each slash

            double radius = 1.8; // Compact X
            double sweep = Math.cos(angle) * radius;
            double drop = Math.sin(angle) * radius;

            // Create layered X slashes
            for (int layer = 0; layer < 3; layer++) {
                double layerDepth = layer * 0.15;

                // First slash: top-left to bottom-right
                Vec3 pos1 = center
                        .add(right.scale(-sweep * 0.7))
                        .add(up.scale(0.6 - drop * 0.8))
                        .add(forward.scale(layerDepth));

                // Second slash: top-right to bottom-left
                Vec3 pos2 = center
                        .add(right.scale(sweep * 0.7))
                        .add(up.scale(0.6 - drop * 0.8))
                        .add(forward.scale(layerDepth));

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos1, color, alpha);
                spawnParticle(level, pos2, color, alpha);
            }
        }
    }

    /**
     * Combo 4: Small lunge attack - quick forward thrust
     */
    private void spawnLungeSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            
            // Lunge motion: starts at player, extends forward rapidly
            double lungeDistance = progress * 2.5; // Forward reach
            double lateralSpread = Math.sin(progress * Math.PI) * 0.4; // Small circular motion

            // Create a tight spiral lunge effect
            for (int layer = 0; layer < 4; layer++) {
                double angle = (progress + layer * 0.25) * Math.PI * 2;
                double spiralRadius = lateralSpread * (1.0 - progress * 0.3); // Tightens as it extends
                
                double xOffset = Math.cos(angle) * spiralRadius;
                double yOffset = Math.sin(angle) * spiralRadius;

                Vec3 pos = center
                        .add(forward.scale(lungeDistance))
                        .add(right.scale(xOffset))
                        .add(up.scale(yOffset));

                Vector3f color = selectColor(layer, 4, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }
}
