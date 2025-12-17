package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders slash particle animations with frame-by-frame progression
 */
public class SlashRenderer {

    // Bright yellow/gold colors
    private static final Vector3f BRIGHT_YELLOW = new Vector3f(1.0f, 0.95f, 0.5f);
    private static final Vector3f GOLD = new Vector3f(1.0f, 0.85f, 0.35f);
    private static final Vector3f LIGHT_GOLD = new Vector3f(1.0f, 0.90f, 0.45f);

    // Particle size
    private static final float PARTICLE_SIZE = 0.7f;

    // Arc configuration
    private static final int MAX_HEIGHT_LAYERS = 7;
    private static final int RADIUS_LAYERS = 5;
    private static final int TOTAL_PARTICLES = 800; // Total particles in full animation, INCREASE FOR MORE MAX FRAMES ESSENTIALLY
    private static final double BASE_RADIUS = 1.5;
    private static final double MAX_RADIUS = 4.0;
    private static final double LAYER_SPACING = 0.12;
    private static final double RADIUS_SPACING = 0.35;

    public enum SlashType {
        RAISED_RIGHT,
        RAISED_LEFT,
        FLAT,
        OVERHEAD
    }

    /**
     * Spawn slash particle arc based on combo hit number
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, int comboHit) {
        SlashType slashType;
        switch (comboHit) {
            case 1 -> slashType = SlashType.RAISED_RIGHT;
            case 2 -> slashType = SlashType.RAISED_LEFT;
            case 3 -> slashType = SlashType.FLAT;
            case 4 -> slashType = SlashType.OVERHEAD;
            default -> slashType = SlashType.RAISED_RIGHT;
        }

        SlashAnimation.startSlashAnimation(level, player, slashType);
    }

    /**
     * Spawn a frame of the slash animation (called per tick by SlashAnimation)
     * @param startParticle Starting particle index for this frame
     * @param particleCount Number of particles to spawn this frame
     * @param animProgress Overall animation progress (0.0 to 1.0)
     */
    public static void spawnSlashFrame(ServerLevel level, Vec3 basePos, Vec3 lookDir,
                                       SlashType slashType, int startParticle,
                                       int particleCount, float animProgress) {

        // Calculate directional vectors
        Vec3 forward = new Vec3(lookDir.x, 0, lookDir.z).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // Arc center position
        Vec3 arcCenter = getArcCenter(basePos, up, slashType);

        // Check if height progression should be reversed
        boolean reverseHeight = shouldReverseHeight(slashType);

        // Spawn particles for this frame
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            // Calculate where this particle is in the overall arc
            double arcProgress = (double) particleIndex / TOTAL_PARTICLES;

            // Height progress
            double heightProgress = reverseHeight ? 1.0 - arcProgress : arcProgress;

            // Calculate layers for this particle
            int heightLayersAtPoint = Math.max(1, (int) Math.ceil(heightProgress * MAX_HEIGHT_LAYERS));
            double maxHeightSpread = heightProgress * (MAX_HEIGHT_LAYERS - 1) * LAYER_SPACING;

            // Determine which layer this particle belongs to
            int layerCycle = particleIndex % (MAX_HEIGHT_LAYERS * RADIUS_LAYERS);
            int vLayer = layerCycle / RADIUS_LAYERS;
            int rLayer = layerCycle % RADIUS_LAYERS;

            // Only spawn if within current layer count
            if (vLayer >= heightLayersAtPoint) continue;

            // Calculate vertical offset
            double verticalOffset = 0;
            if (heightLayersAtPoint > 1) {
                verticalOffset = (vLayer - (heightLayersAtPoint - 1) / 2.0) *
                        (maxHeightSpread / (heightLayersAtPoint - 1));
            }

            // Calculate radius with offset
            double radiusOffset = rLayer * RADIUS_SPACING;
            double radiusMultiplier = Math.sin(arcProgress * Math.PI);
            double currentRadius = BASE_RADIUS + radiusOffset +
                    (MAX_RADIUS - BASE_RADIUS - RADIUS_SPACING * RADIUS_LAYERS) * radiusMultiplier;

            // Calculate particle position
            Vec3 particlePos = calculateArcPosition(
                    arcCenter, right, up, forward, currentRadius,
                    arcProgress, verticalOffset, slashType
            );

            // Select color
            Vector3f color = selectColor(vLayer, rLayer, particleIndex, heightLayersAtPoint);

            // Apply fade effect at end of animation
            float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
            float size = PARTICLE_SIZE * alpha;

            DustParticleOptions dustOptions = new DustParticleOptions(color, size);

            // Spawn particle
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

    private static Vec3 getArcCenter(Vec3 basePos, Vec3 up, SlashType slashType) {
        return switch (slashType) {
            case RAISED_RIGHT, RAISED_LEFT, FLAT ->
                    basePos.add(0, 1.4, 0); // Approximate player height * 0.6
            case OVERHEAD ->
                    basePos.add(0, 2.4, 0); // Approximate player height * 1.2
        };
    }

    private static boolean shouldReverseHeight(SlashType slashType) {
        return switch (slashType) {
            case RAISED_RIGHT -> false;
            case RAISED_LEFT -> true;
            case FLAT -> false;
            case OVERHEAD -> false;
        };
    }

    private static Vec3 calculateArcPosition(Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                             double radius, double progress,
                                             double verticalLayerOffset, SlashType slashType) {
        double angle = progress * Math.PI;
        double arcSweep = Math.cos(angle) * radius;
        double arcForward = Math.sin(angle) * radius;

        return switch (slashType) {
            case RAISED_RIGHT -> {
                double heightRise = progress * radius * 0.4;
                yield basePos
                        .add(right.scale(-arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }

            case RAISED_LEFT -> {
                double heightRise = (1.0 - progress) * radius * 0.4;
                yield basePos
                        .add(right.scale(-arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }

            case FLAT -> {
                yield basePos
                        .add(right.scale(-arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(verticalLayerOffset));
            }

            case OVERHEAD -> {
                double verticalDrop = Math.cos(angle) * radius * 0.8;
                double forwardReach = Math.sin(angle) * radius;
                yield basePos
                        .add(right.scale(arcSweep * 0.2))
                        .add(forward.scale(forwardReach))
                        .add(up.scale(-verticalDrop + verticalLayerOffset));
            }
        };
    }

    private static Vector3f selectColor(int vLayer, int rLayer, int particleIndex, int totalHeightLayers) {
        int combinedIndex = vLayer + rLayer + particleIndex;
        boolean isEdgeLayer = (vLayer == 0 || vLayer == totalHeightLayers - 1);

        if (rLayer >= RADIUS_LAYERS - 1 || isEdgeLayer) {
            return BRIGHT_YELLOW;
        } else if (rLayer == 0) {
            return GOLD;
        } else {
            return (combinedIndex % 2 == 0) ? LIGHT_GOLD : GOLD;
        }
    }
}