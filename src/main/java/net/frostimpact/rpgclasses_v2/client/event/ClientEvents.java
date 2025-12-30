package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.client.ModKeybindings;
import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketUseAbility;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatAllocationScreen;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        
        // Only handle when no screen is open and player exists
        if (mc.screen != null || mc.player == null) {
            return;
        }
        
        if (ModKeybindings.TOGGLE_STATS.consumeClick()) {
            LOGGER.debug("Stats keybind pressed");
            StatsDropdownOverlay.toggleDropdown();
        }
        
        if (ModKeybindings.OPEN_STAT_ALLOCATION.consumeClick()) {
            LOGGER.debug("Stat allocation keybind pressed");
            mc.setScreen(new StatAllocationScreen());
        }
        
        // Handle ability keybinds - send to server
        PlayerRPGData rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        if (ModKeybindings.ABILITY_1.consumeClick()) {
            LOGGER.debug("Ability 1 (Z) pressed for class: {}", currentClass);
            useAbility(1);
        }
        
        if (ModKeybindings.ABILITY_2.consumeClick()) {
            LOGGER.debug("Ability 2 (X) pressed for class: {}", currentClass);
            useAbility(2);
        }
        
        if (ModKeybindings.ABILITY_3.consumeClick()) {
            LOGGER.debug("Ability 3 (C) pressed for class: {}", currentClass);
            useAbility(3);
        }
        
        if (ModKeybindings.ABILITY_4.consumeClick()) {
            LOGGER.debug("Ability 4 (V) pressed for class: {}", currentClass);
            useAbility(4);
        }
    }
    
    private static void useAbility(int abilitySlot) {
        // Send to server for processing
        ModMessages.sendToServer(new PacketUseAbility(abilitySlot));
    }
}