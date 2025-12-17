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
            default -> {
                // Default to first combo attack if invalid combo hit
                System.err.println("Invalid combo hit for Shortsword: " + comboHit + ", defaulting to combo 1");
                spawnCenteredRightSlash(level, basePos, forward, right, up, startParticle, particleCount, animProgress);
            }
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

            // Add gap between particles
            double progress = calculateProgressWithGap(particleIndex, TOTAL_PARTICLES);
            if (progress > 1.0) break;
            
            double angle = progress * Math.PI; // 180 degree arc

            // Centered arc - extends equally around player
            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.6; // More wrap-around
            double heightRise = progress * radius * 0.25; // Slight upward rise

            // Single particle per position - creates clean, visible line
            Vec3 pos = center
                    .add(right.scale(-arcSweep * 0.9)) // Left to right (centered)
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise));

            // Gradient: white at swing edge (leading edge of the slash direction)
            Vector3f color = getGradientColor(progress);
            
            // Constant width: 4 pixels
            float width = 4.0f;
            spawnParticle(level, pos, color, alpha, width);
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

            // Add gap between particles
            double progress = calculateProgressWithGap(particleIndex, TOTAL_PARTICLES);
            if (progress > 1.0) break;
            
            double angle = progress * Math.PI; // 180 degree arc

            // Centered arc - extends equally around player
            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.6; // More wrap-around
            double heightRise = (1.0 - progress) * radius * 0.25; // Slight upward rise

            // Single particle per position - creates clean, visible line
            Vec3 pos = center
                    .add(right.scale(arcSweep * 0.9)) // Right to left (centered)
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise));

            // Gradient: white at swing edge (leading edge of the slash direction)
            Vector3f color = getGradientColor(progress);
            
            // Constant width: 4 pixels
            float width = 4.0f;
            spawnParticle(level, pos, color, alpha, width);
        }
    }

    /**
     * Combo 3: X-pattern dual slash in front of player
     * Creates two diagonal lines that cross in the middle forming an X
     */
    private void spawnXSlash(ServerLevel level, Vec3 basePos, Vec3 forward, Vec3 right, Vec3 up,
                            int startParticle, int particleCount, float animProgress) {
        Vec3 center = basePos.add(forward.scale(1.0)).add(0, 1.3, 0); // Positioned in front at chest level
        float easedProgress = getEasedProgress(animProgress);
        float alpha = calculateAlpha(animProgress);

        // Split particles between two slashes
        int particlesPerSlash = particleCount / 2;
        int halfTotalParticles = TOTAL_PARTICLES / 2;

        for (int i = 0; i < particlesPerSlash; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= halfTotalParticles) break;

            // Linear progress from 0 to 1 for each slash - use double to avoid precision loss
            // Add gap between particles
            double progress = calculateProgressWithGap(particleIndex, halfTotalParticles);
            if (progress > 1.0) break;
            
            // X slash dimensions - wider and more visible
            double slashWidth = 2.0; // Width of the X
            double slashHeight = 2.0; // Height of the X
            
            // First diagonal: top-left to bottom-right (\)
            double x1 = -slashWidth * 0.5 + progress * slashWidth;
            double y1 = slashHeight * 0.5 - progress * slashHeight;
            
            // Second diagonal: bottom-left to top-right (/)
            double x2 = -slashWidth * 0.5 + progress * slashWidth;
            double y2 = -slashHeight * 0.5 + progress * slashHeight;

            // Single particle per position - creates clean, visible lines
            // First slash (\) - white at leading edge (progress direction)
            Vec3 pos1 = center
                    .add(right.scale(x1))
                    .add(up.scale(y1));
            
            // Second slash (/) - white at leading edge (progress direction)
            Vec3 pos2 = center
                    .add(right.scale(x2))
                    .add(up.scale(y2));

            // Gradient color - white at the swing edge (progress direction)
            Vector3f color = getGradientColor(progress);
            
            // Constant width: 4 pixels
            float width = 4.0f;
            
            spawnParticle(level, pos1, color, alpha, width);
            spawnParticle(level, pos2, color, alpha, width);
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

            // Add gap between particles
            double progress = calculateProgressWithGap(particleIndex, TOTAL_PARTICLES);
            if (progress > 1.0) break;
            
            // Lunge motion: starts at player, extends forward rapidly
            double lungeDistance = progress * 2.5; // Forward reach
            double lateralSpread = Math.sin(progress * Math.PI) * 0.4; // Small circular motion

            // Single particle per position - creates clean spiral lunge effect
            double angle = progress * Math.PI * 2;
            double spiralRadius = lateralSpread * (1.0 - progress * 0.3); // Tightens as it extends
            
            double xOffset = Math.cos(angle) * spiralRadius;
            double yOffset = Math.sin(angle) * spiralRadius;

            Vec3 pos = center
                    .add(forward.scale(lungeDistance))
                    .add(right.scale(xOffset))
                    .add(up.scale(yOffset));

            // Gradient: white at the tip (leading edge)
            Vector3f color = getGradientColor(progress);
            
            // Constant width: 4 pixels
            float width = 4.0f;
            spawnParticle(level, pos, color, alpha, width);
        }
    }
}
