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
    private int tickCounter = 0;

    // Track last known stats for each player to avoid recalculating every tick
    private final Map<UUID, Double> lastMoveSpeedStats = new HashMap<>();
    private final Map<UUID, Double> lastAttackSpeedStats = new HashMap<>();
    private final Map<UUID, Integer> lastMaxHealthStats = new HashMap<>();
    private final Map<UUID, Integer> lastDefenseStats = new HashMap<>();
    private final Map<UUID, Integer> lastDamageStats = new HashMap<>();
    
    // Track last known player level for stat point awards
    private final Map<UUID, Integer> lastPlayerLevels = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        tickCounter++;

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            // Tick cooldowns
            var rpgData = player.getData(ModAttachments.PLAYER_RPG);
            rpgData.tickCooldowns();

            // Tick stat modifiers
            var stats = player.getData(ModAttachments.PLAYER_STATS);
            stats.tick();

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
}