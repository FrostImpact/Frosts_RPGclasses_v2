package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders slash particle animations - matches reference images
 */
public class SlashRenderer {
    // Bright yellow/white colors like in reference images
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 1.0f, 0.6f);
    private static final Vector3f BRIGHT_WHITE = new Vector3f(1.0f, 1.0f, 1.0f);

    public enum SlashType {
        DIAGONAL_UP,      // Hit 1: Upward diagonal slash
        DIAGONAL_DOWN,    // Hit 2: Downward diagonal slash
        HORIZONTAL,       // Hit 3: Horizontal sweep
        VERTICAL_DOWN     // Hit 4: Big vertical downward slash
    }

    /**
     * Spawn slash particle arc based on combo hit number
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, int comboHit) {
        SlashType slashType;
        switch (comboHit) {
            case 1 -> slashType = SlashType.DIAGONAL_UP;
            case 2 -> slashType = SlashType.DIAGONAL_DOWN;
            case 3 -> slashType = SlashType.HORIZONTAL;
            case 4 -> slashType = SlashType.VERTICAL_DOWN;
            default -> slashType = SlashType.DIAGONAL_UP;
        }

        spawnSlashArc(level, player, slashType);
    }

    /**
     * Spawn an arc of particles based on slash type - EXACT COPY of reference images
     */
    private static void spawnSlashArc(ServerLevel level, Player player, SlashType slashType) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.getEyePosition();

        // MANY particles for continuous arc (like reference images)
        int particleCount = 60;

        // Small particle size (0.7f) - like reference
        float particleSize = 0.7f;

        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;

            // Calculate position along the arc
            Vec3 particlePos = calculateArcPosition(playerPos, lookVec, slashType, progress);

            // Alternate between bright yellow and bright white
            Vector3f color = (i % 2 == 0) ? BRIGHT_YELLOW : BRIGHT_WHITE;
            DustParticleOptions dustOptions = new DustParticleOptions(color, particleSize);

            // Spawn 2 particles at each point for density
            for (int j = 0; j < 2; j++) {
                level.sendParticles(
                        dustOptions,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                );
            }
        }
    }

    /**
     * Calculate position along arc - COPYING reference image patterns
     */
    private static Vec3 calculateArcPosition(Vec3 origin, Vec3 lookVec, SlashType slashType, double progress) {
        // Get perpendicular vectors
        Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // Base position moves forward slightly
        Vec3 basePos = origin.add(lookVec.scale(1.5));

        return switch (slashType) {
            case DIAGONAL_UP -> {
                // Upward swoosh from bottom-left to top-right
                // Like a sword swinging upward
                double t = progress;
                double curve = Math.sin(t * Math.PI) * 0.3; // Slight curve

                // Start bottom-left, end top-right
                double horizontal = -0.8 + t * 1.6;  // -0.8 to 0.8
                double vertical = -0.5 + t * 1.3;    // -0.5 to 0.8 (upward)

                yield basePos
                        .add(right.scale(horizontal))
                        .add(up.scale(vertical + curve));
            }

            case DIAGONAL_DOWN -> {
                // Downward swoosh from top-left to bottom-right
                double t = progress;
                double curve = Math.sin(t * Math.PI) * 0.3;

                // Start top-left, end bottom-right
                double horizontal = -0.8 + t * 1.6;  // -0.8 to 0.8
                double vertical = 0.8 - t * 1.3;     // 0.8 to -0.5 (downward)

                yield basePos
                        .add(right.scale(horizontal))
                        .add(up.scale(vertical + curve));
            }

            case HORIZONTAL -> {
                // Wide horizontal sweep - like image 3
                double t = progress;

                // Much wider horizontal sweep
                double horizontal = -1.2 + t * 2.4;  // -1.2 to 1.2 (WIDE)
                double vertical = Math.sin(t * Math.PI) * 0.2; // Slight wave

                yield basePos
                        .add(right.scale(horizontal))
                        .add(up.scale(vertical));
            }

            case VERTICAL_DOWN -> {
                // Big vertical downward slash - like image 2
                double t = progress;

                // Starts high, goes down in a slight arc
                double angle = t * Math.PI * 0.8; // Most of a semicircle
                double radius = 1.2;

                // Arc from top to bottom
                double horizontal = Math.sin(angle) * radius * 0.3; // Slight horizontal curve
                double vertical = Math.cos(angle) * radius;         // Big vertical movement

                // Transform so it starts at top (1.0) and ends at bottom (-0.2)
                vertical = 1.0 - vertical;

                yield basePos
                        .add(right.scale(horizontal))
                        .add(up.scale(vertical));
            }
        };
    }
}