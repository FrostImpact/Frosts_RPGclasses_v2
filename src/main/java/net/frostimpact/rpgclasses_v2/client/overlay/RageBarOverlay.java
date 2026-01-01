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
 * Overlay that displays Berserker's RAGE bar
 * Shows current RAGE level and enraged state
 */
public class RageBarOverlay implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 10;
    private static final int BORDER_WIDTH = 1;
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 120;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Only show for Berserker class
        if (currentClass == null || !currentClass.equalsIgnoreCase("berserker")) return;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        
        // Position in top-right area
        int x = screenWidth - MARGIN_RIGHT - BAR_WIDTH - 20;
        int y = MARGIN_TOP;
        
        int rage = rpgData.getRage();
        boolean enraged = rpgData.isEnraged();
        boolean enhancedEnraged = rpgData.isEnhancedEnraged();
        boolean exhausted = rpgData.isExhausted();
        
        RenderSystem.enableBlend();
        
        // Draw background panel
        int panelHeight = 50;
        int bgColor = exhausted ? 0xC0303030 : (enhancedEnraged ? 0xC0400000 : (enraged ? 0xC0302000 : 0xC0200000));
        guiGraphics.fill(x - 5, y - 5, x + BAR_WIDTH + 25, y + panelHeight, bgColor);
        
        // Draw border
        int borderColor = exhausted ? 0xFF505050 : (enhancedEnraged ? 0xFFFF4400 : (enraged ? 0xFFFF8800 : 0xFF804000));
        guiGraphics.fill(x - 5, y - 5, x + BAR_WIDTH + 25, y - 4, borderColor); // Top
        guiGraphics.fill(x - 5, y + panelHeight - 1, x + BAR_WIDTH + 25, y + panelHeight, borderColor); // Bottom
        guiGraphics.fill(x - 5, y - 5, x - 4, y + panelHeight, borderColor); // Left
        guiGraphics.fill(x + BAR_WIDTH + 24, y - 5, x + BAR_WIDTH + 25, y + panelHeight, borderColor); // Right
        
        // Draw title with state indicator
        String title;
        if (exhausted) {
            title = "§8💢 EXHAUSTED";
        } else if (enhancedEnraged) {
            title = "§6§l💢 UNBOUND CARNAGE!";
        } else if (enraged) {
            title = "§c§l💢 ENRAGED!";
        } else {
            title = "§6💢 RAGE";
        }
        guiGraphics.drawString(mc.font, title, x, y, 0xFFFFFF, true);
        
        // Draw RAGE bar
        int barY = y + 15;
        
        // Background (black)
        guiGraphics.fill(x, barY, x + BAR_WIDTH + 20, barY + BAR_HEIGHT, 0xFF000000);
        
        // Fill based on rage
        float ragePercent = rage / 100.0f;
        int fillWidth = (int) ((BAR_WIDTH + 18) * ragePercent);
        
        // Color gradient based on rage amount and state
        int fillColor;
        if (exhausted) {
            fillColor = 0xFF404040; // Gray when exhausted
        } else if (enhancedEnraged) {
            // Pulsing red-orange for enhanced enraged
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 100.0) * 0.5 + 0.5);
            int r = (int) (255 * (0.8f + pulse * 0.2f));
            int g = (int) (128 * pulse);
            fillColor = 0xFF000000 | (r << 16) | (g << 8);
        } else if (enraged) {
            // Orange gradient when enraged
            fillColor = 0xFFFF6600;
        } else {
            // Color based on rage amount (orange to red)
            int r = 255;
            int g = (int) (200 * (1.0f - ragePercent * 0.7f));
            fillColor = 0xFF000000 | (r << 16) | (g << 8);
        }
        
        guiGraphics.fill(x + 1, barY + 1, x + 1 + fillWidth, barY + BAR_HEIGHT - 1, fillColor);
        
        // Draw rage value text
        String rageText = rage + "/100";
        int textX = x + (BAR_WIDTH + 20) / 2 - mc.font.width(rageText) / 2;
        guiGraphics.drawString(mc.font, rageText, textX, barY + 1, 0xFFFFFF, true);
        
        // Draw state info
        int infoY = barY + BAR_HEIGHT + 3;
        if (exhausted) {
            guiGraphics.drawString(mc.font, "§7-20% Speed | No RAGE Gen", x, infoY, 0xFFFFFF, false);
        } else if (enhancedEnraged) {
            guiGraphics.drawString(mc.font, "§6+35% Speed/Dmg | §cIMMORTAL", x, infoY, 0xFFFFFF, false);
        } else if (enraged) {
            guiGraphics.drawString(mc.font, "§6+25% Speed | +30% Dmg | 5% Lifesteal", x, infoY, 0xFFFFFF, false);
        } else {
            int chargesText = rpgData.getAxeThrowCharges();
            guiGraphics.drawString(mc.font, "§7Axe Charges: §e" + chargesText + "/2", x, infoY, 0xFFFFFF, false);
        }
        
        RenderSystem.disableBlend();
    }
}
