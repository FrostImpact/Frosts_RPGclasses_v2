package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Handles client-side events for the stats dropdown
 */
@EventBusSubscriber(modid = "rpgclasses_v2", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        // Only handle left clicks when no screen is open (in-game HUD)
        if (event.getButton() == 0 && event.getAction() == 1) { // Left click, press action
            Minecraft mc = Minecraft.getInstance();
            
            // Only handle when no screen is open
            if (mc.screen == null && mc.player != null) {
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();
                
                // Get mouse position in GUI scaled coordinates
                double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
                double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();
                
                if (StatsDropdownOverlay.isMouseOverButton((int)mouseX, (int)mouseY, screenWidth)) {
                    StatsDropdownOverlay.toggleDropdown();
                    event.setCanceled(true);
                }
            }
        }
    }
}
