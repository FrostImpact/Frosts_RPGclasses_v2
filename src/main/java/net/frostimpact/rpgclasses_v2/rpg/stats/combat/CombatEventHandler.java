package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.SwordItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles combat events for sword weapons
 */
public class CombatEventHandler {
    // Track attack cooldowns per player
    private static final Map<UUID, Integer> attackCooldowns = new HashMap<>();

    /**
     * Disable block breaking with left click for swords
     */
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof SwordItem) {
            event.setCanceled(true);
        }
    }

    /**
     * Disable jump crits for swords
     */
    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof SwordItem) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0f);
        }
    }


    /**
     * Handle sword attacks with combo system
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof SwordItem)) {
            return;
        }

        // Check attack cooldown
        UUID playerUUID = player.getUUID();
        int currentCooldown = attackCooldowns.getOrDefault(playerUUID, 0);

        if (currentCooldown > 0) {
            event.setCanceled(true);
            return;
        }

        // Calculate cooldown based on attack speed stat
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        double attackSpeedBonus = stats.getStatValue(StatType.ATTACK_SPEED);
        int cooldown = (int) (CombatConfig.BASE_ATTACK_COOLDOWN / (1.0 + attackSpeedBonus / 100.0));
        attackCooldowns.put(playerUUID, cooldown);

        // Update combo
        ComboTracker.ComboState combo = ComboTracker.getComboState(playerUUID);
        combo.incrementCombo();
        int comboHit = combo.getComboCount();

        System.out.println("[COMBAT] Player " + player.getName().getString() + " attacked - Combo: " + comboHit);

        // Start slash animation - now animated over time!
        if (player.level() instanceof ServerLevel serverLevel) {
            SlashRenderer.spawnSlashParticles(serverLevel, player, comboHit);
        }
    }

    /**
     * Apply damage multiplier for combo finisher and DAMAGE stat
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.getItem() instanceof SwordItem) {
                ComboTracker.ComboState combo = ComboTracker.getComboState(player.getUUID());
                int comboHit = combo.getComboCount();

                PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
                double damageBonus = stats.getStatValue(StatType.DAMAGE);
                float baseDamage = event.getOriginalDamage();

                float finalDamage = baseDamage * (1.0f + (float)(damageBonus / 100.0));

                if (comboHit == 4) {
                    finalDamage *= CombatConfig.COMBO_FINISHER_MULTIPLIER;
                    System.out.println("[COMBAT] COMBO FINISHER! Damage: " + finalDamage);
                }

                event.setNewDamage(finalDamage);
            }
        }
    }

    /**
     * Tick cooldowns and combos
     */
    public static void tick(Player player) {
        UUID playerUUID = player.getUUID();

        attackCooldowns.computeIfPresent(playerUUID, (uuid, cooldown) -> {
            int newCooldown = cooldown - 1;
            return newCooldown > 0 ? newCooldown : null;
        });

        ComboTracker.tick(playerUUID);
    }

    /**
     * Clean up player data on logout
     */
    public static void removePlayer(UUID playerUUID) {
        attackCooldowns.remove(playerUUID);
        ComboTracker.removePlayer(playerUUID);
        SlashAnimation.removePlayer(playerUUID);
    }
}