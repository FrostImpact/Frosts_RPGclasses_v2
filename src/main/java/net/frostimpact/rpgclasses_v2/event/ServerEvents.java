package net.frostimpact.rpgclasses_v2.event;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncMana;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketSyncStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerEvents {
    private static final int MANA_REGEN_INTERVAL = 20; // Regen every second (20 ticks)
    private int tickCounter = 0;

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

            // Apply movement speed modifier
            applyMovementSpeed(player, stats.getStatValue(StatType.MOVE_SPEED));
        });
    }

    private void applyMovementSpeed(ServerPlayer player, double speedModifier) {
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double baseSpeed = 0.1; // Default Minecraft player speed
            double newSpeed = baseSpeed * (1.0 + speedModifier / 100.0);
            speedAttribute.setBaseValue(newSpeed);
        }
    }
}
