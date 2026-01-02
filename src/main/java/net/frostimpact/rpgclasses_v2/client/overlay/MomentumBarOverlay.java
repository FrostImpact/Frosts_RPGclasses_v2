package net.frostimpact.rpgclasses_v2.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Overlay that displays Lancer's Momentum bar
 * Shows current momentum based on velocity
 * Positioned centered above health and mana bars
 */
public class MomentumBarOverlay implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 8;
    private static final int Y_POSITION = 62; // Slightly above health and mana bars (which are at 50)
    private static final int TEXT_Y_OFFSET = 10; // Pixels above bar for text display
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Only show for Lancer class
        if (currentClass == null || !currentClass.equalsIgnoreCase("lancer")) return;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Position centered horizontally above health and mana bars
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight - Y_POSITION;
        
        float momentum = rpgData.getMomentum();
        boolean empowered = rpgData.isEmpoweredAttack();
        
        RenderSystem.enableBlend();
        
        // Draw background (dark)
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000);
        
        // Calculate fill width based on momentum
        float momentumPercent = momentum / 100.0f;
        int fillWidth = (int) (BAR_WIDTH * momentumPercent);
        
        // Color gradient: pale yellow (0xFFFFCC) at low to bright yellow (0xFFFF00) at high
        int fillColor;
        if (empowered) {
            // Pulsing bright yellow/white when empowered
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 100.0) * 0.5 + 0.5);
            int r = 255;
            int g = 255;
            int b = (int) (200 + pulse * 55); // Pulse between yellow and white
            fillColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else {
            // Gradient from pale yellow (0xFFFFCC) to bright yellow (0xFFFF00)
            int r = 255;
            int g = 255;
            int b = (int) (204 * (1.0f - momentumPercent)); // 204 at 0% momentum, 0 at 100% momentum
            fillColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        
        // Fill the bar
        if (fillWidth > 0) {
            guiGraphics.fill(x + 1, y + 1, x + fillWidth - 1, y + BAR_HEIGHT - 1, fillColor);
        }
        
        // Draw border
        int borderColor = empowered ? 0xFFFFFF00 : 0xFFCCCCCC;
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + BAR_HEIGHT, borderColor); // Left
        guiGraphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, borderColor); // Right
        
        // Draw text
        String text;
        if (empowered) {
            text = "§e§lEMPOWERED!";
        } else {
            text = String.format("§eMomentum: §6%.0f", momentum);
        }
        int textX = x + BAR_WIDTH / 2 - mc.font.width(text) / 2;
        guiGraphics.drawString(mc.font, text, textX, y - TEXT_Y_OFFSET, 0xFFFFFF, true);
        
        RenderSystem.disableBlend();
    }
}
