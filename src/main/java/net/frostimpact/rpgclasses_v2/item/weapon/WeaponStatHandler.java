package net.frostimpact.rpgclasses_v2.item.weapon;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles applying and removing weapon stats when equipment changes
 */
public class WeaponStatHandler {
    // Track what weapon each player has equipped
    private static final Map<UUID, ItemStack> equippedWeapons = new HashMap<>();
    
    /**
     * Check if player's held item has changed and update stats accordingly
     */
    public static void tick(ServerPlayer player) {
        ItemStack currentWeapon = player.getMainHandItem();
        ItemStack previousWeapon = equippedWeapons.get(player.getUUID());

        // Check if weapon changed
        boolean weaponChanged = false;

        if (previousWeapon == null && !currentWeapon.isEmpty()) {
            // Player picked up a weapon
            weaponChanged = true;
        } else if (previousWeapon != null && currentWeapon.isEmpty()) {
            // Player dropped weapon
            weaponChanged = true;
        } else if (previousWeapon != null && !currentWeapon.isEmpty()) {
            // Check if it's a different ITEM TYPE (ignore NBT/components changes)
            if (previousWeapon.getItem() != currentWeapon.getItem()) {
                weaponChanged = true;
            }
        }

        if (weaponChanged) {
            updateWeaponStats(player, previousWeapon, currentWeapon);
        }
    }
    
    /**
     * Update weapon stats when equipment changes
     */
    private static void updateWeaponStats(ServerPlayer player, ItemStack oldWeapon, ItemStack newWeapon) {
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);

        // Remove old weapon stats
        System.out.println("[WEAPON STATS] Before removal, total modifiers: " + stats.getModifiers().size());
        stats.removeAllFromSource(MeleeWeaponItem.WEAPON_STAT_SOURCE);
        System.out.println("[WEAPON STATS] After removal, total modifiers: " + stats.getModifiers().size());

        // Apply new weapon stats
        if (!newWeapon.isEmpty() && newWeapon.getItem() instanceof MeleeWeaponItem newMelee) {
            WeaponStats newStats = newMelee.getWeaponStats();

            System.out.println("[WEAPON STATS] Applying new stats for: " + newWeapon.getItem().toString());

            // Apply each stat bonus as a permanent modifier
            for (Map.Entry<StatType, Double> entry : newStats.getAllBonuses().entrySet()) {
                StatType statType = entry.getKey();
                double value = entry.getValue();

                StatModifier modifier = new StatModifier(
                        MeleeWeaponItem.WEAPON_STAT_SOURCE,
                        statType,
                        value,
                        -1
                );

                stats.addModifier(modifier);
                System.out.println("[WEAPON STATS]   Added: " + statType + " = " + value);
            }
        }

        System.out.println("[WEAPON STATS] Final modifier count: " + stats.getModifiers().size());

        // Sync stats to client
        ModMessages.sendToPlayer(
                new PacketSyncStats(stats.getModifiers()),
                player
        );

        // Update tracked weapon
        if (newWeapon.isEmpty()) {
            equippedWeapons.remove(player.getUUID());
        } else {
            equippedWeapons.put(player.getUUID(), newWeapon.copy());
        }
    }
    
    /**
     * Clean up player data on logout
     */
    public static void removePlayer(UUID playerUUID) {
        equippedWeapons.remove(playerUUID);
    }
}