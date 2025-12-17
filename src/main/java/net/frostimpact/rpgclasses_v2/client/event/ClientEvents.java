package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {

        // Only handle left clicks (button 0) when pressed (action 1)
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Only handle when no screen is open and player exists
        if (mc.screen != null || mc.player == null) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Get mouse position in GUI scaled coordinates
        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();

        if (StatsDropdownOverlay.isMouseOverButton((int)mouseX, (int)mouseY, screenWidth)) {
            LOGGER.debug("Stats dropdown button clicked at ({}, {})", mouseX, mouseY);
            StatsDropdownOverlay.toggleDropdown();
            event.setCanceled(true);
        }
    }
}