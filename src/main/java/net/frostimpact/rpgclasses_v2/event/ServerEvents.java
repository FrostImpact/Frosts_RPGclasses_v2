package net.frostimpact.rpgclasses_v2.event;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncCooldowns;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncSeekerCharges;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEvents.class);
    private static final int MANA_REGEN_INTERVAL = 20; // Regen every second (20 ticks)
    private static final int SEEKER_CHARGE_INTERVAL = 40; // Gain seeker charge every 2 seconds while airborne
    private int tickCounter = 0;

    // Track last known stats for each player to avoid recalculating every tick
    private final Map<UUID, Double> lastMoveSpeedStats = new HashMap<>();
    private final Map<UUID, Double> lastAttackSpeedStats = new HashMap<>();
    private final Map<UUID, Integer> lastMaxHealthStats = new HashMap<>();
    private final Map<UUID, Integer> lastDefenseStats = new HashMap<>();
    private final Map<UUID, Integer> lastDamageStats = new HashMap<>();
    
    // Track last known player level for stat point awards
    private final Map<UUID, Integer> lastPlayerLevels = new HashMap<>();
    
    // Track airborne ticks for seeker charge restoration
    private final Map<UUID, Integer> airborneTickCounter = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        tickCounter++;
        
        // Tick timed ability effects (Rain of Arrows, Seeker projectiles)
        ModMessages.tickTimedEffects();

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            // Tick cooldowns
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            rpgData.tickCooldowns();

            // Tick stat modifiers
            var stats = player.getData(ModAttachments.PLAYER_STATS);
            stats.tick();
            
            // HAWKEYE AERIAL AFFINITY & GLIDE PASSIVE: Restore seeker charges while mid-air + auto-apply Slow Falling
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("hawkeye")) {
                if (!player.onGround() && !player.isInWater() && !player.isInLava()) {
                    // Player is airborne
                    int airTicks = airborneTickCounter.getOrDefault(player.getUUID(), 0) + 1;
                    airborneTickCounter.put(player.getUUID(), airTicks);
                    
                    // GLIDE PASSIVE: Auto-apply Slow Falling I while airborne
                    if (!player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 40, 0, false, false));
                    }
                    
                    // Every SEEKER_CHARGE_INTERVAL ticks while airborne, add a seeker charge
                    if (airTicks % SEEKER_CHARGE_INTERVAL == 0) {
                        int oldCharges = rpgData.getSeekerCharges();
                        rpgData.addSeekerCharge();
                        int newCharges = rpgData.getSeekerCharges();
                        
                        if (newCharges > oldCharges) {
                            // Sync to client
                            ModMessages.sendToPlayer(new PacketSyncSeekerCharges(newCharges), player);
                            LOGGER.debug("Hawkeye {} gained aerial seeker charge, now has {}", 
                                    player.getName().getString(), newCharges);
                        }
                    }
                } else {
                    // Player is grounded - reset counter
                    airborneTickCounter.put(player.getUUID(), 0);
                }
            }
            
            // BEAST MASTER BEAST BOND PASSIVE: +5% damage per active beast companion (max 3 stacks = +15%)
            if (rpgData.getCurrentClass() != null && rpgData.getCurrentClass().equalsIgnoreCase("beastmaster")) {
                // Count active summoned beasts nearby (within 30 blocks)
                int activeBeastCount = countNearbyBeastCompanions(player);
                
                // Remove old beast bond modifier
                stats.removeModifier("beast_bond", net.frostimpact.rpgclasses_v2.rpg.stats.StatType.DAMAGE);
                
                // Apply new beast bond modifier if beasts are present
                if (activeBeastCount > 0) {
                    int maxStacks = 3;
                    int effectiveBeastCount = Math.min(activeBeastCount, maxStacks);
                    int damageBonus = effectiveBeastCount * 5; // 5% per beast, max 15%
                    
                    stats.addModifier(new net.frostimpact.rpgclasses_v2.rpg.stats.StatModifier(
                            "beast_bond",
                            net.frostimpact.rpgclasses_v2.rpg.stats.StatType.DAMAGE,
                            damageBonus,
                            -1 // Permanent until removed
                    ));
                }
            }

            // Mana regeneration and cooldown sync
            if (tickCounter % MANA_REGEN_INTERVAL == 0) {
                int manaRegenBonus = stats.getIntStatValue(StatType.MANA_REGEN);
                int baseRegen = 1;
                int regenAmount = baseRegen + manaRegenBonus;

                int oldMana = rpgData.getMana();
                rpgData.regenMana(regenAmount);

                // Sync if mana changed
                if (rpgData.getMana() != oldMana) {
                    ModMessages.sendToPlayer(
                            new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()),
                            player
                    );
                }
                
                // Sync cooldowns periodically
                ModMessages.sendToPlayer(new PacketSyncCooldowns(rpgData.getAllCooldowns()), player);
            }

            // Apply movement speed modifier only if it changed
            double currentSpeedStat = stats.getPercentageStatValue(StatType.MOVE_SPEED);

            Double lastSpeedStat = lastMoveSpeedStats.get(player.getUUID());
            if (lastSpeedStat == null || !lastSpeedStat.equals(currentSpeedStat)) {
                applyMovementSpeed(player, currentSpeedStat);
                lastMoveSpeedStats.put(player.getUUID(), currentSpeedStat);
            }
            
            // Apply attack speed modifier only if it changed
            double currentAttackSpeedStat = stats.getPercentageStatValue(StatType.ATTACK_SPEED);
            Double lastAttackSpeedStat = lastAttackSpeedStats.get(player.getUUID());
            if (lastAttackSpeedStat == null || !lastAttackSpeedStat.equals(currentAttackSpeedStat)) {
                applyAttackSpeed(player, currentAttackSpeedStat);
                lastAttackSpeedStats.put(player.getUUID(), currentAttackSpeedStat);
            }
            
            // Apply max health modifier only if it changed
            int currentMaxHealthStat = stats.getIntStatValue(StatType.MAX_HEALTH);
            Integer lastMaxHealthStat = lastMaxHealthStats.get(player.getUUID());
            if (lastMaxHealthStat == null || !lastMaxHealthStat.equals(currentMaxHealthStat)) {
                applyMaxHealth(player, currentMaxHealthStat);
                lastMaxHealthStats.put(player.getUUID(), currentMaxHealthStat);
            }
            
            // Apply defense modifier only if it changed
            int currentDefenseStat = stats.getIntStatValue(StatType.DEFENSE);
            Integer lastDefenseStat = lastDefenseStats.get(player.getUUID());
            if (lastDefenseStat == null || !lastDefenseStat.equals(currentDefenseStat)) {
                applyDefense(player, currentDefenseStat);
                lastDefenseStats.put(player.getUUID(), currentDefenseStat);
            }
            
            // Apply damage modifier only if it changed
            int currentDamageStat = stats.getIntStatValue(StatType.DAMAGE);
            Integer lastDamageStat = lastDamageStats.get(player.getUUID());
            if (lastDamageStat == null || !lastDamageStat.equals(currentDamageStat)) {
                applyDamage(player, currentDamageStat);
                lastDamageStats.put(player.getUUID(), currentDamageStat);
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            lastMoveSpeedStats.remove(playerUUID);
            lastAttackSpeedStats.remove(playerUUID);
            lastMaxHealthStats.remove(playerUUID);
            lastDefenseStats.remove(playerUUID);
            lastDamageStats.remove(playerUUID);
            lastPlayerLevels.remove(playerUUID);
            airborneTickCounter.remove(playerUUID);
            // Clean up any active Rain of Arrows effects for this player
            ModMessages.getActiveRainEffects().remove(playerUUID);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync all RPG data to client on login
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            var stats = player.getData(ModAttachments.PLAYER_STATS);
            
            ModMessages.sendToPlayer(new PacketSyncMana(rpgData.getMana(), rpgData.getMaxMana()), player);
            ModMessages.sendToPlayer(new PacketSyncStats(stats.getModifiers()), player);
            ModMessages.sendToPlayer(new PacketSyncRPGData(
                    rpgData.getCurrentClass(),
                    rpgData.getLevel(),
                    rpgData.getClassLevel(),
                    rpgData.getClassExperience(),
                    rpgData.getAvailableStatPoints(),
                    rpgData.getAvailableSkillPoints()
            ), player);
            // Sync seeker charges for Hawkeye class
            ModMessages.sendToPlayer(new PacketSyncSeekerCharges(rpgData.getSeekerCharges()), player);
            
            // Initialize last level tracking
            lastPlayerLevels.put(player.getUUID(), player.experienceLevel);
            
            LOGGER.debug("Synced RPG data for player {} on login", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int newLevel = player.experienceLevel + event.getLevels();
            int oldLevel = player.experienceLevel;
            
            // Only award points when gaining levels (not losing)
            if (newLevel > oldLevel) {
                var rpgData = player.getData(ModAttachments.PLAYER_RPG);
                
                // Award 1 stat point per level gained
                int levelsGained = newLevel - oldLevel;
                rpgData.addStatPoints(levelsGained);
                
                // Update the stored level
                rpgData.setLevel(newLevel);
                
                LOGGER.info("Player {} gained {} level(s), now has {} stat points", 
                        player.getName().getString(), levelsGained, rpgData.getAvailableStatPoints());
                
                // Sync to client
                ModMessages.sendToPlayer(new PacketSyncRPGData(
                        rpgData.getCurrentClass(),
                        rpgData.getLevel(),
                        rpgData.getClassLevel(),
                        rpgData.getClassExperience(),
                        rpgData.getAvailableStatPoints(),
                        rpgData.getAvailableSkillPoints()
                ), player);
            }
        }
    }

    private void applyMovementSpeed(ServerPlayer player, double speedModifier) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double baseSpeed = 0.1;
            double newSpeed = baseSpeed * (1.0 + speedModifier / 100.0);

            LOGGER.debug("Applying MOVE_SPEED to player {}: modifier={}%, baseSpeed={}, newSpeed={}",
                    player.getName().getString(), speedModifier, baseSpeed, newSpeed);

            speedAttribute.setBaseValue(newSpeed);
        }
    }
    
    private void applyAttackSpeed(ServerPlayer player, double attackSpeedModifier) {
        AttributeInstance attackSpeedAttribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttribute != null) {
            double baseAttackSpeed = 4.0; // Minecraft's base attack speed
            double newAttackSpeed = baseAttackSpeed * (1.0 + attackSpeedModifier / 100.0);

            LOGGER.debug("Applying ATTACK_SPEED to player {}: modifier={}%, baseSpeed={}, newSpeed={}",
                    player.getName().getString(), attackSpeedModifier, baseAttackSpeed, newAttackSpeed);

            attackSpeedAttribute.setBaseValue(newAttackSpeed);
        }
    }
    
    private void applyMaxHealth(ServerPlayer player, int healthBonus) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double baseMaxHealth = 20.0; // Minecraft's base max health
            double newMaxHealth = baseMaxHealth + healthBonus;

            LOGGER.debug("Applying MAX_HEALTH to player {}: bonus={}, baseHealth={}, newHealth={}",
                    player.getName().getString(), healthBonus, baseMaxHealth, newMaxHealth);

            maxHealthAttribute.setBaseValue(newMaxHealth);
            
            // Heal the player if their health is now below the new max
            if (player.getHealth() > newMaxHealth) {
                player.setHealth((float) newMaxHealth);
            }
        }
    }
    
    private void applyDefense(ServerPlayer player, int defenseBonus) {
        AttributeInstance armorAttribute = player.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            double baseArmor = 0.0; // Minecraft's base armor without equipment
            double newArmor = baseArmor + defenseBonus;

            LOGGER.debug("Applying DEFENSE to player {}: bonus={}, baseArmor={}, newArmor={}",
                    player.getName().getString(), defenseBonus, baseArmor, newArmor);

            armorAttribute.setBaseValue(newArmor);
        }
    }
    
    private void applyDamage(ServerPlayer player, int damageBonus) {
        AttributeInstance attackDamageAttribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttribute != null) {
            double baseAttackDamage = 1.0; // Minecraft's base attack damage
            double newAttackDamage = baseAttackDamage + damageBonus;

            LOGGER.debug("Applying DAMAGE to player {}: bonus={}, baseDamage={}, newDamage={}",
                    player.getName().getString(), damageBonus, baseAttackDamage, newAttackDamage);

            attackDamageAttribute.setBaseValue(newAttackDamage);
        }
    }
    
    /**
     * Count nearby beast companions summoned by the player (for Beast Bond passive)
     */
    private int countNearbyBeastCompanions(ServerPlayer player) {
        double searchRadius = 30.0;
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        
        java.util.List<net.minecraft.world.entity.Entity> entities = player.level().getEntities(player, searchBox,
                e -> e instanceof net.minecraft.world.entity.Mob);
        
        int count = 0;
        for (net.minecraft.world.entity.Entity entity : entities) {
            // Check if entity is marked as a summoned beast
            if (entity.getPersistentData().getBoolean("rpgclasses_summoned_beast")) {
                // Verify owner matches (if owner is stored)
                if (entity.getPersistentData().contains("rpgclasses_owner")) {
                    java.util.UUID ownerUUID = entity.getPersistentData().getUUID("rpgclasses_owner");
                    if (ownerUUID.equals(player.getUUID()) && entity.isAlive()) {
                        count++;
                    }
                } else if (entity instanceof net.minecraft.world.entity.TamableAnimal tameable) {
                    // For tameable animals (wolves), check if owned by player
                    if (tameable.getOwnerUUID() != null && tameable.getOwnerUUID().equals(player.getUUID()) && 
                            tameable.getPersistentData().getBoolean("rpgclasses_summoned_beast")) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
}