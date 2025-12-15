package net.frostimpact.rpgclasses_v2.event;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.frostimpact.rpgclasses_v2.rpg.stats.combat.CombatEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
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
                double manaRegenBonus = stats.getStatValue(StatType.MANA_REGEN);
                int baseRegen = 1;
                int regenAmount = baseRegen + (int) (baseRegen * manaRegenBonus / 100.0);
                
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
            double currentSpeedStat = stats.getStatValue(StatType.MOVE_SPEED);

            Double lastSpeedStat = lastMoveSpeedStats.get(player.getUUID());
            if (lastSpeedStat == null || !lastSpeedStat.equals(currentSpeedStat)) {
                applyMovementSpeed(player, currentSpeedStat);
                lastMoveSpeedStats.put(player.getUUID(), currentSpeedStat);
            }
            
            // Tick combat system (attack cooldowns and combo tracker)
            CombatEventHandler.tick(player);
        });


    }



    private void applyMovementSpeed(ServerPlayer player, double speedModifier) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double baseSpeed = 0.1;

            System.out.println("[SERVER] BASE SPEED IS " + baseSpeed);

            double newSpeed = baseSpeed * (1.0 + speedModifier / 100.0);

            System.out.println("[SERVER] NEW SPEED IS " + newSpeed);
            System.out.println("[SERVER] SPEED MODIFIER IS" + speedModifier);
            
            LOGGER.debug("Applying MOVE_SPEED to player {}: modifier={}%, baseSpeed={}, newSpeed={}", 
                player.getName().getString(), speedModifier, baseSpeed, newSpeed);
            
            speedAttribute.setBaseValue(newSpeed);
        }
    }


}
