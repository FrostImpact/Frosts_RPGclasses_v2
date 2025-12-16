package net.frostimpact.rpgclasses_v2.client.event;

import net.frostimpact.rpgclasses_v2.RpgClassesMod;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatsDropdownOverlay;
import net.frostimpact.rpgclasses_v2.rpg.stats.combat.SlashRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Handles client-side events for the stats dropdown
 */
@EventBusSubscriber(modid = RpgClassesMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {



    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        // Only handle left clicks (button 0) when pressed (action 1)
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Only handle when no screen is open (in-game HUD) and player exists
        if (mc.screen != null || mc.player == null) {
            return;
        }

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