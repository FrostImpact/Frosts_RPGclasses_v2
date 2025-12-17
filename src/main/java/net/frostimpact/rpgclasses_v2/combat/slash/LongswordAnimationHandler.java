package net.frostimpact.rpgclasses_v2.combat.slash;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Animation handler for Longsword weapons
 * Features: Balanced, versatile slashes with good reach
 */
public class LongswordAnimationHandler extends WeaponAnimationHandler {
    private static final int TOTAL_PARTICLES = 700;
    private static final double BASE_RADIUS = 1.5;
    private static final double MAX_RADIUS = 3.2; // Reduced for smaller slashes

    @Override
    public int getMaxComboCount() {
        return 4;
    }

    @Override
    public float getAnimationSpeedMultiplier() {
        return 1.0f; // Normal speed
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
            case 1 -> spawnRaisedRightSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 2 -> spawnRaisedLeftSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 3 -> spawnFlatSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            case 4 -> spawnOverheadSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
        }
    }

    /**
     * Combo 1: Left to right sweep, angled upward
     */
    private void spawnRaisedRightSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                      int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.65; // Good wrap-around
            double heightRise = progress * radius * 0.3;

            for (int layer = 0; layer < 3; layer++) {
                double layerOffset = layer * 0.22;
                
                Vec3 pos = center
                        .add(right.scale(-arcSweep * 0.95))
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }

    /**
     * Combo 2: Right to left sweep, angled upward
     */
    private void spawnRaisedLeftSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                     int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.65; // Good wrap-around
            double heightRise = (1.0 - progress) * radius * 0.3;

            for (int layer = 0; layer < 3; layer++) {
                double layerOffset = layer * 0.22;
                
                Vec3 pos = center
                        .add(right.scale(arcSweep * 0.95))
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(heightRise));

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }

    /**
     * Combo 3: Left to right, completely flat horizontal
     */
    private void spawnFlatSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                               int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 1.3, 0);
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.65; // Good wrap-around

            for (int layer = 0; layer < 3; layer++) {
                double layerOffset = layer * 0.22;
                // Add slight vertical layers for thickness
                double verticalSpread = (layer - 1) * 0.15;
                
                Vec3 pos = center
                        .add(right.scale(arcSweep * 0.95))
                        .add(forward.scale(arcForward - layerOffset))
                        .add(up.scale(verticalSpread)); // Flat but with thickness

                Vector3f color = selectColor(layer, 3, layer == 0);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }

    /**
     * Combo 4: Over the head downward slash
     */
    private void spawnOverheadSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                                   int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(0, 2.3, 0); // Higher for overhead
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * 0.85;
            double verticalDrop = Math.cos(angle) * radius * 0.75;
            double forwardReach = Math.sin(angle) * radius * 0.85;

            for (int layer = 0; layer < 3; layer++) {
                double sideSpread = (layer - 1) * 0.18;
                
                Vec3 pos = center
                        .add(right.scale(sideSpread))
                        .add(forward.scale(forwardReach))
                        .add(up.scale(-verticalDrop));

                Vector3f color = selectColor(layer, 3, layer == 0 || layer == 2);
                spawnParticle(level, pos, color, alpha);
            }
        }
    }
}
