package net.frostimpact.rpgclasses_v2.combat.slash;

import net.frostimpact.rpgclasses_v2.item.weapon.WeaponType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Manages active slash animations that play over multiple ticks
 */
public class SlashAnimation {
    private static final Map<UUID, List<ActiveSlash>> activeSlashes = new HashMap<>();
    private static final Map<WeaponType, WeaponAnimationHandler> handlers = new HashMap<>();

    static {
        // Initialize weapon-specific handlers
        handlers.put(WeaponType.SHORTSWORD, new ShortswordAnimationHandler());
        handlers.put(WeaponType.LONGSWORD, new LongswordAnimationHandler());
        handlers.put(WeaponType.CLAYMORE, new ClaymoreAnimationHandler());
    }

    // Animation configuration - Base values
    private static final int BASE_ANIMATION_DURATION = 8;
    private static final int BASE_PARTICLES_PER_FRAME = 100;

    /**
     * Represents an active slash animation
     */
    private static class ActiveSlash {
        final ServerLevel level;
        final Player player;
        final WeaponType weaponType;
        final int comboHit;
        final Vec3 startPos;
        final Vec3 lookDir;
        final int duration;
        final int particlesPerFrame;
        final WeaponAnimationHandler handler;
        int currentTick = 0;
        int particlesSpawned = 0;

        ActiveSlash(ServerLevel level, Player player, WeaponType weaponType, int comboHit) {
            this.level = level;
            this.player = player;
            this.weaponType = weaponType;
            this.comboHit = comboHit;
            this.startPos = player.position();
            this.lookDir = player.getLookAngle();
            this.handler = handlers.get(weaponType);

            // Ensure handler exists
            if (this.handler == null) {
                throw new IllegalStateException("No animation handler found for weapon type: " + weaponType);
            }

            // Adjust animation speed based on weapon handler
            float speedMultiplier = handler.getAnimationSpeedMultiplier();
            this.duration = (int) (BASE_ANIMATION_DURATION * speedMultiplier);
            this.particlesPerFrame = (int) (BASE_PARTICLES_PER_FRAME / speedMultiplier);
        }

        boolean isComplete() {
            return currentTick >= duration;
        }

        void tick() {
            currentTick++;

            // Calculate animation progress (0.0 to 1.0)
            float progress = (float) currentTick / duration;

            // Spawn particles for this frame using weapon-specific handler
            handler.spawnComboParticles(
                    level,
                    startPos,
                    lookDir,
                    comboHit,
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
    public static void startSlashAnimation(ServerLevel level, Player player, WeaponType weaponType, int comboHit) {
        UUID playerUUID = player.getUUID();

        // Get or create list for this player
        List<ActiveSlash> slashes = activeSlashes.computeIfAbsent(playerUUID, k -> new ArrayList<>());

        // Add new slash animation with weapon-specific handler
        slashes.add(new ActiveSlash(level, player, weaponType, comboHit));
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