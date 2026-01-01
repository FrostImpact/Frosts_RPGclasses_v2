package net.frostimpact.rpgclasses_v2.entity;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which entities have been hit by each player.
 * This is used to determine whether to show the health bar for an entity.
 */
@EventBusSubscriber(modid = RpgClassesMod.MOD_ID)
public class HitEntityTracker {
    
    // Map of player UUID -> Set of entity UUIDs they have hit
    private static final Map<UUID, Set<UUID>> playerHitEntities = new ConcurrentHashMap<>();
    
    // Client-side set of entities the local player has hit (this is accessed on both sides,
    // the isClientSide check ensures only client adds to it)
    private static final Set<UUID> clientHitEntities = ConcurrentHashMap.newKeySet();
    
    /**
     * Called when an entity takes damage
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        Entity source = event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        
        // Only track if source is a player
        if (source instanceof Player player && target != null) {
            UUID playerUUID = player.getUUID();
            UUID targetUUID = target.getUUID();
            
            // Add to tracking based on which side we're on
            if (player.level().isClientSide()) {
                // Client-side: add to client tracking
                clientHitEntities.add(targetUUID);
                org.slf4j.LoggerFactory.getLogger("HitEntityTracker").debug(
                    "CLIENT: Player {} hit entity {} ({})", 
                    player.getName().getString(), 
                    target.getType().getDescription().getString(),
                    targetUUID);
            } else {
                // Server-side: add to server tracking
                playerHitEntities.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(targetUUID);
                org.slf4j.LoggerFactory.getLogger("HitEntityTracker").debug(
                    "SERVER: Player {} hit entity {} ({})", 
                    player.getName().getString(), 
                    target.getType().getDescription().getString(),
                    targetUUID);
            }
        }
    }
    
    /**
     * Clean up when entity leaves the level (dies or unloads)
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            UUID entityUUID = entity.getUUID();
            // Remove from all player tracking sets
            playerHitEntities.values().forEach(set -> set.remove(entityUUID));
            // Also remove from client-side tracking
            clientHitEntities.remove(entityUUID);
        }
    }
    
    /**
     * Check if a player has hit a specific entity (server-side)
     */
    public static boolean hasPlayerHitEntity(UUID playerUUID, UUID entityUUID) {
        Set<UUID> hitEntities = playerHitEntities.get(playerUUID);
        return hitEntities != null && hitEntities.contains(entityUUID);
    }
    
    /**
     * Check if the local player has hit an entity (client-side)
     */
    public static boolean hasLocalPlayerHitEntity(UUID entityUUID) {
        return clientHitEntities.contains(entityUUID);
    }
    
    /**
     * Add an entity to the local player's hit list (client-side)
     */
    public static void addLocalPlayerHit(UUID entityUUID) {
        clientHitEntities.add(entityUUID);
    }
    
    /**
     * Remove an entity from the local player's hit list (client-side)
     */
    public static void removeLocalPlayerHit(UUID entityUUID) {
        clientHitEntities.remove(entityUUID);
    }
    
    /**
     * Clear all tracking data for a player
     */
    public static void clearPlayerData(UUID playerUUID) {
        playerHitEntities.remove(playerUUID);
    }
    
    /**
     * Clear client-side hit tracking
     */
    public static void clearClientData() {
        clientHitEntities.clear();
    }
}
