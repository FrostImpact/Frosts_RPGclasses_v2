package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpg.stats.combat.slash.SlashAnimation;
import net.frostimpact.rpgclasses_v2.rpg.stats.combat.slash.SlashRenderer;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.MeleeWeaponItem;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.WeaponType;
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
 * Handles combat events for all melee weapons
 */
public class CombatEventHandler {
    private static final Map<UUID, Integer> attackCooldowns = new HashMap<>();

    /**
     * Disable block breaking with left click for melee weapons
     */
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof MeleeWeaponItem) {
            event.setCanceled(true);
        }
    }

    /**
     * Disable jump crits for melee weapons
     */
    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() instanceof MeleeWeaponItem) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0f);
        }
    }

    /**
     * Handle melee weapon attacks with weapon-specific combo systems
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof MeleeWeaponItem meleeWeapon)) {
            return;
        }

        UUID playerUUID = player.getUUID();
        int currentCooldown = attackCooldowns.getOrDefault(playerUUID, 0);

        if (currentCooldown > 0) {
            event.setCanceled(true);
            return;
        }

        // Get weapon type and calculate cooldown with weapon speed multiplier
        WeaponType weaponType = meleeWeapon.getWeaponType();
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        double attackSpeedBonus = stats.getStatValue(StatType.ATTACK_SPEED);

        // Apply weapon type speed multiplier
        int baseCooldown = (int) (CombatConfig.BASE_ATTACK_COOLDOWN * weaponType.getSpeedMultiplier());
        int cooldown = (int) (baseCooldown / (1.0 + attackSpeedBonus / 100.0));
        attackCooldowns.put(playerUUID, cooldown);

        // Update combo with weapon type
        ComboTracker.ComboState combo = ComboTracker.getComboState(playerUUID);
        combo.incrementCombo(weaponType);
        int comboHit = combo.getComboCount();

        System.out.println("[COMBAT] Player " + player.getName().getString() +
                " attacked with " + weaponType + " - Combo: " + comboHit);

        // Start weapon-specific slash animation
        if (player.level() instanceof ServerLevel serverLevel) {
            SlashRenderer.spawnSlashParticles(serverLevel, player, weaponType, comboHit);
        }
    }

    /**
     * Apply damage multiplier for combo finisher and DAMAGE stat
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.getItem() instanceof MeleeWeaponItem meleeWeapon) {
                ComboTracker.ComboState combo = ComboTracker.getComboState(player.getUUID());
                int comboHit = combo.getComboCount();
                WeaponType weaponType = meleeWeapon.getWeaponType();

                PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
                double damageBonus = stats.getStatValue(StatType.DAMAGE);
                float baseDamage = event.getOriginalDamage();

                float finalDamage = baseDamage * (1.0f + (float)(damageBonus / 100.0));

                // Apply finisher multiplier on last hit of combo
                if (comboHit == weaponType.getMaxComboCount()) {
                    finalDamage *= CombatConfig.COMBO_FINISHER_MULTIPLIER;
                    System.out.println("[COMBAT] " + weaponType + " COMBO FINISHER! Damage: " + finalDamage);
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