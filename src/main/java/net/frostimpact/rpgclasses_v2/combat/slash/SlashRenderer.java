package net.frostimpact.rpgclasses_v2.combat.slash;

import net.frostimpact.rpgclasses_v2.item.weapon.WeaponType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders slash particle animations for different weapon types
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
    private static final int TOTAL_PARTICLES = 800;
    private static final double BASE_RADIUS = 1.5;
    private static final double MAX_RADIUS = 4.0;
    private static final double LAYER_SPACING = 0.12;
    private static final double RADIUS_SPACING = 0.35;

    public enum SlashType {
        // SHORTSWORD (3 attacks)
        SHORT_ANGLED_RIGHT,    // Combo 1: Quick angled right
        SHORT_ANGLED_LEFT,     // Combo 2: Quick angled left
        SHORT_DUAL_SLASH,      // Combo 3: Double X slash

        // LONGSWORD (4 attacks)
        LONG_RAISED_RIGHT,     // Combo 1: Left to right sweep, angled upward
        LONG_RAISED_LEFT,      // Combo 2: Right to left sweep, angled upward
        LONG_FLAT,             // Combo 3: Left to right, completely flat horizontal
        LONG_OVERHEAD,         // Combo 4: Over the head downward

        // CLAYMORE (4 attacks)
        CLAY_ANGLED_RIGHT,     // Combo 1: Heavy angled right
        CLAY_ANGLED_LEFT,      // Combo 2: Heavy angled left
        CLAY_OVERHEAD_SMASH,   // Combo 3: Powerful overhead smash
        CLAY_SPIN_AOE          // Combo 4: Full 360 spin attack
    }

    /**
     * Spawn slash based on weapon type and combo hit
     */
    public static void spawnSlashParticles(ServerLevel level, Player player, WeaponType weaponType, int comboHit) {
        SlashType slashType = getSlashType(weaponType, comboHit);
        SlashAnimation.startSlashAnimation(level, player, slashType);
    }

    /**
     * Get the appropriate slash type based on weapon and combo
     */
    private static SlashType getSlashType(WeaponType weaponType, int comboHit) {
        return switch (weaponType) {
            case SHORTSWORD -> switch (comboHit) {
                case 1 -> SlashType.SHORT_ANGLED_RIGHT;
                case 2 -> SlashType.SHORT_ANGLED_LEFT;
                case 3 -> SlashType.SHORT_DUAL_SLASH;
                default -> SlashType.SHORT_ANGLED_RIGHT;
            };
            case LONGSWORD -> switch (comboHit) {
                case 1 -> SlashType.LONG_RAISED_RIGHT;
                case 2 -> SlashType.LONG_RAISED_LEFT;
                case 3 -> SlashType.LONG_FLAT;
                case 4 -> SlashType.LONG_OVERHEAD;
                default -> SlashType.LONG_RAISED_RIGHT;
            };
            case CLAYMORE -> switch (comboHit) {
                case 1 -> SlashType.CLAY_ANGLED_RIGHT;
                case 2 -> SlashType.CLAY_ANGLED_LEFT;
                case 3 -> SlashType.CLAY_OVERHEAD_SMASH;
                case 4 -> SlashType.CLAY_SPIN_AOE;
                default -> SlashType.CLAY_ANGLED_RIGHT;
            };
        };
    }

    /**
     * Spawn a frame of the slash animation
     */
    public static void spawnSlashFrame(ServerLevel level, Vec3 basePos, Vec3 lookDir,
                                       SlashType slashType, int startParticle,
                                       int particleCount, float animProgress) {

        Vec3 forward = new Vec3(lookDir.x, 0, lookDir.z).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        Vec3 arcCenter = getArcCenter(basePos, up, slashType);
        boolean reverseHeight = shouldReverseHeight(slashType);

        // Special handling for dual slash and spin
        if (slashType == SlashType.SHORT_DUAL_SLASH) {
            spawnDualSlashFrame(level, arcCenter, right, up, forward, startParticle, particleCount, animProgress);
            return;
        } else if (slashType == SlashType.CLAY_SPIN_AOE) {
            spawnSpinFrame(level, arcCenter, right, up, forward, startParticle, particleCount, animProgress);
            return;
        }

        // Standard slash rendering
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double arcProgress = (double) particleIndex / TOTAL_PARTICLES;
            double heightProgress = reverseHeight ? 1.0 - arcProgress : arcProgress;

            int heightLayersAtPoint = Math.max(1, (int) Math.ceil(heightProgress * MAX_HEIGHT_LAYERS));
            double maxHeightSpread = heightProgress * (MAX_HEIGHT_LAYERS - 1) * LAYER_SPACING;

            int layerCycle = particleIndex % (MAX_HEIGHT_LAYERS * RADIUS_LAYERS);
            int vLayer = layerCycle / RADIUS_LAYERS;
            int rLayer = layerCycle % RADIUS_LAYERS;

            if (vLayer >= heightLayersAtPoint) continue;

            double verticalOffset = 0;
            if (heightLayersAtPoint > 1) {
                verticalOffset = (vLayer - (heightLayersAtPoint - 1) / 2.0) *
                        (maxHeightSpread / (heightLayersAtPoint - 1));
            }

            double radiusOffset = rLayer * RADIUS_SPACING;
            double radiusMultiplier = Math.sin(arcProgress * Math.PI);
            double currentRadius = BASE_RADIUS + radiusOffset +
                    (MAX_RADIUS - BASE_RADIUS - RADIUS_SPACING * RADIUS_LAYERS) * radiusMultiplier;

            Vec3 particlePos = calculateArcPosition(
                    arcCenter, right, up, forward, currentRadius,
                    arcProgress, verticalOffset, slashType
            );

            Vector3f color = selectColor(vLayer, rLayer, particleIndex, heightLayersAtPoint);
            float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
            float size = PARTICLE_SIZE * alpha;

            DustParticleOptions dustOptions = new DustParticleOptions(color, size);

            level.sendParticles(dustOptions, particlePos.x, particlePos.y, particlePos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Spawn dual slash (X pattern) - spawns two slashes simultaneously
     */
    private static void spawnDualSlashFrame(ServerLevel level, Vec3 arcCenter, Vec3 right, Vec3 up, Vec3 forward,
                                            int startParticle, int particleCount, float animProgress) {
        // Calculate how many particles to spawn for EACH slash
        int particlesPerSlash = Math.max(1, particleCount / 2);

        for (int i = 0; i < particlesPerSlash; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break; // Each slash gets half the total

            double arcProgress = (double) particleIndex / (TOTAL_PARTICLES);

            // Calculate arc sweep with proper curve
            double angle = arcProgress * Math.PI;
            double radius = 2.2;

            // Arc positioning
            double arcSweep = Math.cos(angle) * radius;
            double arcForward = Math.sin(angle) * radius * 0.3;
            double verticalOffset = Math.sin(angle) * radius * 0.8;

            // First slash: Top-left to bottom-right (diagonal down-right)
            Vec3 pos1 = arcCenter
                    .add(right.scale(-arcSweep * 0.8))           // Left to right
                    .add(forward.scale(arcForward))              // Slight forward
                    .add(up.scale(0.5 - verticalOffset));       // Top to bottom

            // Second slash: Top-right to bottom-left (diagonal down-left)
            Vec3 pos2 = arcCenter
                    .add(right.scale(arcSweep * 0.8))            // Right to left
                    .add(forward.scale(arcForward))              // Slight forward
                    .add(up.scale(0.5 - verticalOffset));       // Top to bottom

            // Add height layers for volume
            int heightLayers = Math.max(1, (int)(arcProgress * 5));
            for (int h = 0; h < heightLayers; h++) {
                double heightOffset = (h - heightLayers / 2.0) * 0.15;

                // Add radius layers for thickness
                for (int r = 0; r < 3; r++) {
                    double radiusOffset = r * 0.25;

                    // Calculate adjusted positions with layers
                    Vec3 layeredPos1 = pos1
                            .add(up.scale(heightOffset))
                            .add(forward.scale(-radiusOffset));

                    Vec3 layeredPos2 = pos2
                            .add(up.scale(heightOffset))
                            .add(forward.scale(-radiusOffset));

                    // Color selection
                    Vector3f color;
                    if (r == 0) {
                        color = BRIGHT_YELLOW; // Brightest edge
                    } else if (r == 1) {
                        color = LIGHT_GOLD;
                    } else {
                        color = GOLD;
                    }

                    float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                    float size = PARTICLE_SIZE * 0.85f * alpha;

                    DustParticleOptions dustOptions = new DustParticleOptions(color, size);

                    // Spawn both slashes
                    level.sendParticles(dustOptions, layeredPos1.x, layeredPos1.y, layeredPos1.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                    level.sendParticles(dustOptions, layeredPos2.x, layeredPos2.y, layeredPos2.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    /**
     * Spawn 360-degree spin attack
     */
    private static void spawnSpinFrame(ServerLevel level, Vec3 arcCenter, Vec3 right, Vec3 up, Vec3 forward,
                                       int startParticle, int particleCount, float animProgress) {
        for (int i = 0; i < particleCount; i++) {
            int particleIndex = startParticle + i;
            if (particleIndex >= TOTAL_PARTICLES) break;

            double progress = (double) particleIndex / TOTAL_PARTICLES;

            // Full 360 degree rotation
            double angle = progress * Math.PI * 2;

            // Vary radius slightly for more dynamic effect
            double baseRadius = 3.5;
            double radiusVariation = Math.sin(progress * Math.PI * 8) * 0.3;
            double radius = baseRadius + radiusVariation;

            // Height varies (waist to chest level) with wave pattern
            double heightVariation = Math.sin(progress * Math.PI * 4) * 0.4;

            // Calculate position in circle
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;

            // Add multiple radius layers for thickness and impact
            for (int layer = 0; layer < 4; layer++) {
                double layerRadius = radius - (layer * 0.35);
                double layerX = Math.cos(angle) * layerRadius;
                double layerZ = Math.sin(angle) * layerRadius;

                // Add height layers for vertical thickness
                for (int hLayer = 0; hLayer < 3; hLayer++) {
                    double hOffset = (hLayer - 1) * 0.25;

                    Vec3 layerPos = arcCenter
                            .add(right.scale(layerX))
                            .add(forward.scale(layerZ))
                            .add(up.scale(heightVariation + hOffset));

                    // Outer layers are brightest
                    Vector3f color;
                    if (layer == 0) {
                        color = BRIGHT_YELLOW;
                    } else if (layer == 1) {
                        color = LIGHT_GOLD;
                    } else {
                        color = GOLD;
                    }

                    float alpha = animProgress < 0.8f ? 1.0f : (1.0f - (animProgress - 0.8f) / 0.2f);
                    float size = PARTICLE_SIZE * 1.3f * alpha; // Larger particles for AOE impact

                    DustParticleOptions dustOptions = new DustParticleOptions(color, size);
                    level.sendParticles(dustOptions, layerPos.x, layerPos.y, layerPos.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static Vec3 getArcCenter(Vec3 basePos, Vec3 up, SlashType slashType) {
        return switch (slashType) {
            case SHORT_ANGLED_RIGHT, SHORT_ANGLED_LEFT, SHORT_DUAL_SLASH ->
                    basePos.add(0, 1.4, 0);
            case LONG_RAISED_RIGHT, LONG_RAISED_LEFT, LONG_FLAT ->
                    basePos.add(0, 1.4, 0);
            case LONG_OVERHEAD ->
                    basePos.add(0, 2.4, 0);
            case CLAY_ANGLED_RIGHT, CLAY_ANGLED_LEFT ->
                    basePos.add(0, 1.5, 0);
            case CLAY_OVERHEAD_SMASH ->
                    basePos.add(0, 2.6, 0); // Higher for heavier weapon
            case CLAY_SPIN_AOE ->
                    basePos.add(0, 1.0, 0); // Waist level for spin
        };
    }

    private static boolean shouldReverseHeight(SlashType slashType) {
        return switch (slashType) {
            case SHORT_ANGLED_LEFT, LONG_RAISED_LEFT, CLAY_ANGLED_LEFT -> true;
            default -> false;
        };
    }

    private static Vec3 calculateArcPosition(Vec3 basePos, Vec3 right, Vec3 up, Vec3 forward,
                                             double radius, double progress,
                                             double verticalLayerOffset, SlashType slashType) {
        double angle = progress * Math.PI;
        double arcSweep = Math.cos(angle) * radius;
        double arcForward = Math.sin(angle) * radius;

        return switch (slashType) {
            // SHORTSWORD - Quick, compact slashes
            case SHORT_ANGLED_RIGHT -> {
                double heightRise = progress * radius * 0.35; // Slightly less rise
                yield basePos
                        .add(right.scale(arcSweep * 0.8)) // Shorter arc
                        .add(forward.scale(arcForward * 0.8))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }
            case SHORT_ANGLED_LEFT -> {
                double heightRise = (1 - progress) * radius * 0.35;
                yield basePos
                        .add(right.scale(-arcSweep * 0.8))
                        .add(forward.scale(arcForward * 0.8))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }

            // LONGSWORD - Balanced slashes
            case LONG_RAISED_RIGHT -> {
                double heightRise = progress * radius * 0.4;
                yield basePos
                        .add(right.scale(arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }
            case LONG_RAISED_LEFT -> {
                double heightRise = (1.0 - progress) * radius * 0.4;
                yield basePos
                        .add(right.scale(-arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }
            case LONG_FLAT -> {
                yield basePos
                        .add(right.scale(-arcSweep))
                        .add(forward.scale(arcForward))
                        .add(up.scale(verticalLayerOffset));
            }
            case LONG_OVERHEAD -> {
                double verticalDrop = Math.cos(angle) * radius * 0.8;
                double forwardReach = Math.sin(angle) * radius;
                yield basePos
                        .add(right.scale(arcSweep * 0.2))
                        .add(forward.scale(forwardReach))
                        .add(up.scale(-verticalDrop + verticalLayerOffset));
            }

            // CLAYMORE - Wide, powerful slashes
            case CLAY_ANGLED_RIGHT -> {
                double heightRise = progress * radius * 0.45; // Slightly more rise
                yield basePos
                        .add(right.scale(-arcSweep * 1.2)) // Wider arc
                        .add(forward.scale(arcForward * 1.1))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }
            case CLAY_ANGLED_LEFT -> {
                double heightRise = (1.0 - progress) * radius * 0.45;
                yield basePos
                        .add(right.scale(arcSweep * 1.2))
                        .add(forward.scale(arcForward * 1.1))
                        .add(up.scale(heightRise + verticalLayerOffset));
            }
            case CLAY_OVERHEAD_SMASH -> {
                double verticalDrop = Math.cos(angle) * radius * 1.0; // More vertical
                double forwardReach = Math.sin(angle) * radius * 1.1;
                yield basePos
                        .add(right.scale(arcSweep * 0.1)) // Nearly straight down
                        .add(forward.scale(forwardReach))
                        .add(up.scale(-verticalDrop + verticalLayerOffset));
            }

            default -> basePos; // Should not reach here
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