package net.frostimpact.rpgclasses_v2.event;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncRPGData;
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

    // Track last known move speed stat for each player to avoid recalculating every tick
    private final Map<UUID, Double> lastMoveSpeedStats = new HashMap<>();
    
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

            // Mana regeneration
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
            }

            // Apply movement speed modifier only if it changed
            double currentSpeedStat = stats.getPercentageStatValue(StatType.MOVE_SPEED);

            Double lastSpeedStat = lastMoveSpeedStats.get(player.getUUID());
            if (lastSpeedStat == null || !lastSpeedStat.equals(currentSpeedStat)) {
                applyMovementSpeed(player, currentSpeedStat);
                lastMoveSpeedStats.put(player.getUUID(), currentSpeedStat);
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            lastMoveSpeedStats.remove(playerUUID);
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
}