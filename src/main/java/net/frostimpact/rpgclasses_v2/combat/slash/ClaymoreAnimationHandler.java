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

            // Add gap between particles
            double progress = (double) particleIndex / TOTAL_PARTICLES * PARTICLE_GAP;
            if (progress > 1.0) break;
            
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.7; // More wrap-around
            double heightRise = progress * radius * 0.35;

            // Single particle per position - creates clean, visible line
            Vec3 pos = center
                    .add(right.scale(-arcSweep * 1.0)) // Wide arc
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise));

            // Gradient: white at swing edge
            Vector3f color = getGradientColor(progress);
            
            // Tapered width: gradually increases from 1 to 6 pixels
            float width = calculateTaperedWidth(progress, 1.0f, 6.0f);
            spawnParticle(level, pos, color, alpha * 1.1f, width); // Heavy weapon - more visible
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

            // Add gap between particles
            double progress = (double) particleIndex / TOTAL_PARTICLES * PARTICLE_GAP;
            if (progress > 1.0) break;
            
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * Math.sin(angle);
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.7; // More wrap-around
            double heightRise = (1.0 - progress) * radius * 0.35;

            // Single particle per position - creates clean, visible line
            Vec3 pos = center
                    .add(right.scale(arcSweep * 1.0)) // Wide arc
                    .add(forward.scale(arcForward))
                    .add(up.scale(heightRise));

            // Gradient: white at swing edge
            Vector3f color = getGradientColor(progress);
            
            // Tapered width: gradually increases from 1 to 6 pixels
            float width = calculateTaperedWidth(progress, 1.0f, 6.0f);
            spawnParticle(level, pos, color, alpha * 1.1f, width); // Heavy weapon - more visible
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

            // Add gap between particles
            double progress = (double) particleIndex / TOTAL_PARTICLES * PARTICLE_GAP;
            if (progress > 1.0) break;
            
            double angle = progress * Math.PI;

            double radius = BASE_RADIUS + (MAX_RADIUS - BASE_RADIUS) * 0.9;
            double verticalDrop = Math.cos(angle) * radius * 0.9;
            double forwardReach = Math.sin(angle) * radius * 0.9;

            // Single particle per position - creates clean downward slash
            Vec3 pos = center
                    .add(forward.scale(forwardReach))
                    .add(up.scale(-verticalDrop));

            // Gradient: white at the leading edge (downward swing direction)
            Vector3f color = getGradientColor(progress);
            
            // Tapered width: gradually increases from 1 to 6 pixels
            float width = calculateTaperedWidth(progress, 1.0f, 6.0f);
            spawnParticle(level, pos, color, alpha * 1.1f, width); // Heavy weapon - more visible
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

            // Add gap between particles
            double progress = (double) particleIndex / TOTAL_PARTICLES * PARTICLE_GAP;
            if (progress > 1.0) break;

            // Full 360 degree rotation
            double rotationAngle = progress * Math.PI * 2;
            
            // Fixed radius for consistent ring
            double slashRadius = 3.0;
            
            // Calculate position on the circle
            double xPos = Math.cos(rotationAngle) * slashRadius;
            double zPos = Math.sin(rotationAngle) * slashRadius;
            
            // Single particle per position - creates clean circular slash
            Vec3 pos = center
                    .add(right.scale(xPos))
                    .add(forward.scale(zPos));

            // Gradient: white at start of rotation, transitioning to gold
            Vector3f color = getGradientColor(progress);
            
            // Tapered width: gradually increases from 1 to 6 pixels
            float width = calculateTaperedWidth(progress, 1.0f, 6.0f);
            spawnParticle(level, pos, color, alpha * 1.1f, width); // Heavy weapon - more visible
        }
    }
}
