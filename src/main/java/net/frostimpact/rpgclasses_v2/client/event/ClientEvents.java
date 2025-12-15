package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Handles client-side events for the stats dropdown
 */
@EventBusSubscriber(modid = "rpgclasses_v2", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        // Only handle clicks when no screen is open (in-game HUD)
        if (event.getScreen() == null) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();
            
            // Convert screen coordinates to GUI scaled coordinates
            int scaledMouseX = (int) (mouseX * screenWidth / mc.getWindow().getScreenWidth());
            int scaledMouseY = (int) (mouseY * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
            
            if (StatsDropdownOverlay.isMouseOverButton(scaledMouseX, scaledMouseY, screenWidth)) {
                StatsDropdownOverlay.toggleDropdown();
                event.setCanceled(true);
            }
        }
    }
}
