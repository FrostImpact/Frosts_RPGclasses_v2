package net.frostimpact.rpgclasses_v2.rpg.stats.combat;

import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.PlayerStats;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpg.stats.weapon.SwordItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
            event.setResult(CriticalHitEvent.Result.DENY); // Disable crits
        }
    }
    
    /**
     * Handle sword attacks with combo system
     */
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getMainHandItem();
        
        if (!(heldItem.getItem() instanceof SwordItem)) {
            return;
        }
        
        // Check attack cooldown
        UUID playerUUID = player.getUUID();
        int currentCooldown = attackCooldowns.getOrDefault(playerUUID, 0);
        
        if (currentCooldown > 0) {
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
        
        // Spawn slash particles
        if (player.level() instanceof ServerLevel serverLevel) {
            SlashRenderer.spawnSlashParticles(serverLevel, player, comboHit);
        }
    }
    
    /**
     * Apply damage multiplier for combo finisher and DAMAGE stat
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.getItem() instanceof SwordItem) {
                // Get combo count (should be already incremented by AttackEntityEvent)
                ComboTracker.ComboState combo = ComboTracker.getComboState(player.getUUID());
                int comboHit = combo.getComboCount();
                
                // Only apply if we have an active combo
                if (comboHit > 0) {
                    // Get base damage with DAMAGE stat bonus
                    PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
                    double damageBonus = stats.getStatValue(StatType.DAMAGE);
                    float baseDamage = event.getOriginalDamage();
                    
                    // Apply DAMAGE stat bonus
                    float finalDamage = baseDamage * (1.0f + (float)(damageBonus / 100.0));
                    
                    // Apply finisher multiplier on 4th hit
                    if (comboHit == 4) {
                        finalDamage *= CombatConfig.COMBO_FINISHER_MULTIPLIER;
                    }
                    
                    event.setNewDamage(finalDamage);
                }
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
