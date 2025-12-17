package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Manages active slash animations that play over multiple ticks
 */
public class SlashAnimation {
    private static final Map<UUID, List<ActiveSlash>> activeSlashes = new HashMap<>();

    // Animation configuration - Base values
    private static final int BASE_ANIMATION_DURATION = 8;
    private static final int BASE_PARTICLES_PER_FRAME = 100;

    /**
     * Represents an active slash animation
     */
    private static class ActiveSlash {
        final ServerLevel level;
        final Player player;
        final SlashRenderer.SlashType slashType;
        final Vec3 startPos;
        final Vec3 lookDir;
        final int duration;
        final int particlesPerFrame;
        int currentTick = 0;
        int particlesSpawned = 0;

        ActiveSlash(ServerLevel level, Player player, SlashRenderer.SlashType slashType) {
            this.level = level;
            this.player = player;
            this.slashType = slashType;
            this.startPos = player.position();
            this.lookDir = player.getLookAngle();

            // Adjust animation speed based on slash type
            float speedMultiplier = getSpeedMultiplier(slashType);
            this.duration = (int) (BASE_ANIMATION_DURATION * speedMultiplier);
            this.particlesPerFrame = (int) (BASE_PARTICLES_PER_FRAME / speedMultiplier);
        }

        private float getSpeedMultiplier(SlashRenderer.SlashType slashType) {
            // Determine weapon type from slash type
            String typeName = slashType.name();
            if (typeName.startsWith("SHORT_")) {
                return 0.6f; // Shortsword: 60% duration = faster
            } else if (typeName.startsWith("CLAY_")) {
                return 1.5f; // Claymore: 150% duration = slower
            } else {
                return 1.0f; // Longsword: normal speed
            }
        }

        boolean isComplete() {
            return currentTick >= duration;
        }

        void tick() {
            currentTick++;

            // Calculate animation progress (0.0 to 1.0)
            float progress = (float) currentTick / duration;

            // Spawn particles for this frame
            SlashRenderer.spawnSlashFrame(
                    level,
                    startPos,
                    lookDir,
                    slashType,
                    particlesSpawned,
                    particlesPerFrame,
                    progress
            );

            particlesSpawned += particlesPerFrame;
        }
    }

    /**
     * Start a new slash animation
     */
    public static void startSlashAnimation(ServerLevel level, Player player, SlashRenderer.SlashType slashType) {
        UUID playerUUID = player.getUUID();

        // Get or create list for this player
        List<ActiveSlash> slashes = activeSlashes.computeIfAbsent(playerUUID, k -> new ArrayList<>());

        // Add new slash animation
        slashes.add(new ActiveSlash(level, player, slashType));
    }

    /**
     * Tick all active slash animations - call this from ServerEvents
     */
    public static void tickAnimations() {
        Iterator<Map.Entry<UUID, List<ActiveSlash>>> playerIter = activeSlashes.entrySet().iterator();

        while (playerIter.hasNext()) {
            Map.Entry<UUID, List<ActiveSlash>> entry = playerIter.next();
            List<ActiveSlash> slashes = entry.getValue();

            // Tick all slashes for this player
            Iterator<ActiveSlash> slashIter = slashes.iterator();
            while (slashIter.hasNext()) {
                ActiveSlash slash = slashIter.next();
                slash.tick();

                // Remove completed animations
                if (slash.isComplete()) {
                    slashIter.remove();
                }
            }

            // Remove player entry if no active slashes
            if (slashes.isEmpty()) {
                playerIter.remove();
            }
        }
    }

    /**
     * Clean up player data on logout
     */
    public static void removePlayer(UUID playerUUID) {
        activeSlashes.remove(playerUUID);
    }

    /**
     * Get count of active slashes for debugging
     */
    public static int getActiveSlashCount() {
        return activeSlashes.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}