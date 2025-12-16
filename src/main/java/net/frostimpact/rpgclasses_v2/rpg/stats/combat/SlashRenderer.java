package net. frostimpact.rpgclasses_v2.rpg. stats.combat;

import net. minecraft.core. particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft. world.entity. player.Player;
import net. minecraft.world. phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders slash particle animations with filled volume arcs
 * that start thin and gradually increase in height.
 */
public class SlashRenderer {

    // Bright yellow/gold colors matching reference
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    private static final Vector3f LIGHT_GOLD = new Vector3f(1.0f, 0.90f, 0.45f);

    // Particle size
    private static final float PARTICLE_SIZE = 0.7f;

    // Arc configuration
    private static final int MAX_HEIGHT_LAYERS = 7;     // Maximum vertical layers at the end
    private static final int RADIUS_LAYERS = 5;         // Radial layers to fill volume
    private static final int PARTICLES_PER_LAYER = 40;  // Particles per arc layer
    private static final double BASE_RADIUS = 1.5;      // Inner radius
    private static final double MAX_RADIUS = 4.0;       // Outer radius at peak
    private static final double LAYER_SPACING = 0.12;   // Vertical spacing between layers
    private static final double RADIUS_SPACING = 0.35;  // Radial spacing to fill in

    public enum SlashType {
        RAISED_RIGHT,    // Combo 1: Left to right sweep, angled upward
        RAISED_LEFT,     // Combo 2: Right to left sweep, angled upward
        FLAT,            // Combo 3: Left to right, completely flat horizontal
        OVERHEAD         // Combo 4: Over the head downward
    }

    /**
     * Spawn slash particle arc based on combo hit number
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, int comboHit) {
        SlashType slashType;
        switch (comboHit) {
            case 1 -> slashType = SlashType.RAISED_RIGHT;
            case 2 -> slashType = SlashType. RAISED_LEFT;
            case 3 -> slashType = SlashType.FLAT;
            case 4 -> slashType = SlashType.OVERHEAD;
            default -> slashType = SlashType.RAISED_RIGHT;
        }

        spawnSlashArc(level, player, slashType);
    }

    /**
     * Spawn filled slash arc that starts thin and grows in height.
     * Arc spawns ON the player with combo-based orientations.
     */
    private static void spawnSlashArc(ServerLevel level, Player player, SlashType slashType) {
        // Get player's horizontal look direction
        Vec3 lookDir = player.getLookAngle();
        Vec3 forward = new Vec3(lookDir.x, 0, lookDir.z).normalize();

        // Calculate right vector (perpendicular to forward on horizontal plane)
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // Arc center position based on slash type
        Vec3 arcCenter = getArcCenter(player, up, slashType);

        // Check if height progression should be reversed
        boolean reverseHeight = shouldReverseHeight(slashType);

        // Spawn particles along the arc
        for (int i = 0; i < PARTICLES_PER_LAYER; i++) {
            double progress = (double) i / PARTICLES_PER_LAYER;

            // Height progress - can be reversed based on slash direction
            double heightProgress = reverseHeight ? 1.0 - progress : progress;

            // Calculate how many height layers at this point (grows from 1 to MAX)
            int heightLayersAtPoint = Math.max(1, (int) Math.ceil(heightProgress * MAX_HEIGHT_LAYERS));

            // Calculate the height spread at this point
            double maxHeightSpread = heightProgress * (MAX_HEIGHT_LAYERS - 1) * LAYER_SPACING;

            // Spawn vertical layers (fewer at start, more at end)
            for (int vLayer = 0; vLayer < heightLayersAtPoint; vLayer++) {
                // Center the layers vertically
                double verticalOffset = 0;
                if (heightLayersAtPoint > 1) {
                    verticalOffset = (vLayer - (heightLayersAtPoint - 1) / 2.0) *
                            (maxHeightSpread / (heightLayersAtPoint - 1));
                }

                // Spawn multiple radial layers to fill in the volume
                for (int rLayer = 0; rLayer < RADIUS_LAYERS; rLayer++) {
                    double radiusOffset = rLayer * RADIUS_SPACING;

                    // Calculate dynamic radius with fill offset
                    double radiusMultiplier = Math. sin(progress * Math.PI);
                    double currentRadius = BASE_RADIUS + radiusOffset +
                            (MAX_RADIUS - BASE_RADIUS - RADIUS_SPACING * RADIUS_LAYERS) * radiusMultiplier;

                    // Calculate particle position along the arc
                    Vec3 particlePos = calculateArcPosition(
                            arcCenter, right, up, forward, currentRadius,
                            progress, verticalOffset, slashType
                    );

                    // Color based on layer depth for 3D effect
                    Vector3f color = selectColor(vLayer, rLayer, i, heightLayersAtPoint);

                    DustParticleOptions dustOptions = new DustParticleOptions(color, PARTICLE_SIZE);

                    // Spawn particle at computed position
                    level.sendParticles(
                            dustOptions,
                            particlePos.x,
                            particlePos. y,
                            particlePos.z,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                    );
                }
            }
        }
    }

    /**
     * Get the arc center position based on slash type
     */
    private static Vec3 getArcCenter(Player player, Vec3 up, SlashType slashType) {
        return switch (slashType) {
            case RAISED_RIGHT, RAISED_LEFT, FLAT ->
                    player.position().add(0, player.getBbHeight() * 0.6, 0);
            case OVERHEAD ->
                    player.position().add(0, player.getBbHeight() * 1.2, 0); // Above head
        };
    }

    /**
     * Determine if height progression should be reversed based on slash type.
     * Height starts thin at the BEGINNING of the slash motion.
     */
    private static boolean shouldReverseHeight(SlashType slashType) {
        return switch (slashType) {
            case RAISED_RIGHT -> false; // Starts left (thin) -> ends right (thick)
            case RAISED_LEFT -> true;   // Starts right (thin) -> ends left (thick)
            case FLAT -> false;         // Starts left (thin) -> ends right (thick)
            case OVERHEAD -> false;     // Starts top (thin) -> ends bottom (thick)
        };
    }

    /**
     * Calculate position along arc based on slash type.
     */
    private static Vec3 calculateArcPosition(Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                             double radius, double progress,
                                             double verticalLayerOffset, SlashType slashType) {
        // Arc angle sweep (crescent shape - about 180 degrees)
        double angle = progress * Math.PI;

        // Base arc coordinates
        double arcSweep = Math.cos(angle) * radius;    // Sweep direction
        double arcForward = Math.sin(angle) * radius;  // Forward curve at peak

        return switch (slashType) {
            case RAISED_RIGHT -> {
                // Left to right sweep, rising upward
                // Starts on LEFT (negative X), ends on RIGHT (positive X)
                double heightRise = progress * radius * 0.4;
                yield basePos
                        .add(right.scale(-arcSweep)) // Flip so it goes left to right
                        .add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }

            case RAISED_LEFT -> {
                // Right to left sweep, rising upward
                // Starts on RIGHT (positive X), ends on LEFT (negative X)
                double heightRise = (1.0 - progress) * radius * 0.4; // Reverse rise
                yield basePos
                        .add(right.scale(-arcSweep)) // Normal direction (right to left)
                        . add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }

            case FLAT -> {
                // Left to right, completely flat horizontal sweep
                yield basePos
                        .add(right.scale(-arcSweep)) // Flip so it goes left to right
                        .add(forward.scale(arcForward))
                        .add(up. scale(verticalLayerOffset)); // Only layer offset, no rise
            }

            case OVERHEAD -> {
                // Over the head, sweeping downward in front
                double verticalDrop = Math.cos(angle) * radius * 0.8;
                double forwardReach = Math.sin(angle) * radius;
                yield basePos
                        .add(right.scale(arcSweep * 0.2)) // Minimal side movement
                        .add(forward. scale(forwardReach))
                        .add(up.scale(-verticalDrop + verticalLayerOffset));
            }
        };
    }

    /**
     * Select color based on layer position for depth effect
     */
    private static Vector3f selectColor(int vLayer, int rLayer, int particleIndex, int totalHeightLayers) {
        int combinedIndex = vLayer + rLayer + particleIndex;

        // Edges brighter for glow effect
        boolean isEdgeLayer = (vLayer == 0 || vLayer == totalHeightLayers - 1);

        if (rLayer >= RADIUS_LAYERS - 1 || isEdgeLayer) {
            // Outer edge or top/bottom - brightest
            return BRIGHT_YELLOW;
        } else if (rLayer == 0) {
            // Inner edge - gold
            return GOLD;
        } else {
            // Middle layers - alternating
            return (combinedIndex % 2 == 0) ? LIGHT_GOLD : GOLD;
        }
    }
}