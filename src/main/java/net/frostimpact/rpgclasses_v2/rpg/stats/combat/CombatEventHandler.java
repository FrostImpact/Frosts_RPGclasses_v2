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
            event.setCanceled(true); // Prevent block breaking
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

        // CRITICAL: Only process on server side!
        if (player.level().isClientSide()) {
            System.out.println("[COMBAT] Client-side attack event, ignoring");
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        System.out.println("[COMBAT] SERVER-SIDE Attack event fired! Item: " + heldItem.getItem().getClass().getSimpleName());

        if (!(heldItem.getItem() instanceof SwordItem)) {
            System.out.println("[COMBAT] Not a sword item, ignoring");
            return;
        }

        System.out.println("[COMBAT] Sword attack detected!");

        // Check attack cooldown
        UUID playerUUID = player.getUUID();
        int currentCooldown = attackCooldowns.getOrDefault(playerUUID, 0);

        if (currentCooldown > 0) {
            System.out.println("[COMBAT] Attack on cooldown (" + currentCooldown + " ticks remaining)");
            event.setCanceled(true); // Attack on cooldown
            return;
        }

        // Calculate cooldown based on attack speed stat
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        double attackSpeedBonus = stats.getStatValue(StatType.ATTACK_SPEED);
        int cooldown = (int) (CombatConfig.BASE_ATTACK_COOLDOWN / (1.0 + attackSpeedBonus / 100.0));
        attackCooldowns.put(playerUUID, cooldown);

        // Update combo BEFORE damage is applied
        ComboTracker.ComboState combo = ComboTracker.getComboState(playerUUID);
        combo.incrementCombo();
        int comboHit = combo.getComboCount();

        System.out.println("[COMBAT] Player " + player.getName().getString() + " attacked - Combo: " + comboHit);

        // Spawn slash particles - we're guaranteed to be server-side here
        if (player.level() instanceof ServerLevel serverLevel) {
            System.out.println("[COMBAT] Spawning particles on server...");
            SlashRenderer.spawnSlashParticles(serverLevel, player, comboHit);
        } else {
            System.out.println("[COMBAT] ERROR: Server-side but not ServerLevel?! Level class: " + player.level().getClass().getSimpleName());
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
                // Get combo count (should be already incremented by AttackEntityEvent)
                ComboTracker.ComboState combo = ComboTracker.getComboState(player.getUUID());
                int comboHit = combo.getComboCount();

                // Get base damage with DAMAGE stat bonus
                PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
                double damageBonus = stats.getStatValue(StatType.DAMAGE);
                float baseDamage = event.getOriginalDamage();

                // Apply DAMAGE stat bonus
                float finalDamage = baseDamage * (1.0f + (float)(damageBonus / 100.0));

                // Apply finisher multiplier on 4th hit
                if (comboHit == 4) {
                    finalDamage *= CombatConfig.COMBO_FINISHER_MULTIPLIER;
                    System.out.println("[COMBAT] COMBO FINISHER! Damage: " + finalDamage);
                }

                event.setNewDamage(finalDamage);

                // Debug log
                System.out.println("[COMBAT] Damage applied: " + finalDamage + " (base: " + baseDamage + ", bonus: " + damageBonus + "%)");
            }
        }
    }

    /**
     * Tick cooldowns and combos
     */
    public static void tick(Player player) {
        UUID playerUUID = player.getUUID();

        // Tick attack cooldown
        attackCooldowns.computeIfPresent(playerUUID, (uuid, cooldown) -> {
            int newCooldown = cooldown - 1;
            return newCooldown > 0 ? newCooldown : null;
        });

        // Tick combo tracker
        ComboTracker.tick(playerUUID);
    }

    /**
     * Clean up player data on logout
     */
    public static void removePlayer(UUID playerUUID) {
        attackCooldowns.remove(playerUUID);
        ComboTracker.removePlayer(playerUUID);
    }
}