package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.client.ModKeybindings;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatAllocationScreen;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
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
    }
}