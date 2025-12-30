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
 * Overlay that displays Hawkeye seeker charges in a visually appealing way
 * Shows circular orbs representing available seeker charges
 */
public class SeekerChargeOverlay implements LayeredDraw.Layer {
    private static final int MAX_CHARGES = PlayerRPGData.MAX_SEEKER_CHARGES;
    private static final int ORB_SIZE = 14;
    private static final int ORB_SPACING = 4;
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 80;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Only show for Hawkeye class
        if (currentClass == null || !currentClass.equalsIgnoreCase("hawkeye")) return;
        
        int seekerCharges = rpgData.getSeekerCharges();
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        
        // Position in top-right area
        int startX = screenWidth - MARGIN_RIGHT - (MAX_CHARGES * (ORB_SIZE + ORB_SPACING));
        int y = MARGIN_TOP;
        
        RenderSystem.enableBlend();
        
        // Draw title
        String title = "Seekers";
        int titleWidth = mc.font.width(title);
        guiGraphics.drawString(mc.font, title, startX + (MAX_CHARGES * (ORB_SIZE + ORB_SPACING)) / 2 - titleWidth / 2, 
                y - 12, 0xBB88FF, true);
        
        // Draw charge orbs
        for (int i = 0; i < MAX_CHARGES; i++) {
            int orbX = startX + i * (ORB_SIZE + ORB_SPACING);
            boolean hasCharge = i < seekerCharges;
            
            drawSeekerOrb(guiGraphics, orbX, y, hasCharge, i, seekerCharges);
        }
        
        // Draw charge count text
        String chargeText = seekerCharges + "/" + MAX_CHARGES;
        int textWidth = mc.font.width(chargeText);
        int textColor = seekerCharges > 0 ? 0xBB88FF : 0x666666;
        guiGraphics.drawString(mc.font, chargeText, 
                startX + (MAX_CHARGES * (ORB_SIZE + ORB_SPACING)) / 2 - textWidth / 2,
                y + ORB_SIZE + 3, textColor, false);
        
        RenderSystem.disableBlend();
    }
    
    private void drawSeekerOrb(GuiGraphics guiGraphics, int x, int y, boolean active, int index, int totalCharges) {
        // Background circle (always visible)
        int bgColor = 0x80222233;
        drawCircle(guiGraphics, x + ORB_SIZE / 2, y + ORB_SIZE / 2, ORB_SIZE / 2, bgColor);
        
        if (active) {
            // Active orb - purple/magenta gradient effect
            // Outer glow
            int glowColor = 0x40BB66FF;
            drawCircle(guiGraphics, x + ORB_SIZE / 2, y + ORB_SIZE / 2, ORB_SIZE / 2 + 2, glowColor);
            
            // Main orb - purple core
            int coreColor = 0xFFAA55FF;
            drawCircle(guiGraphics, x + ORB_SIZE / 2, y + ORB_SIZE / 2, ORB_SIZE / 2 - 1, coreColor);
            
            // Inner highlight
            int highlightColor = 0xFFDD99FF;
            drawCircle(guiGraphics, x + ORB_SIZE / 2 - 2, y + ORB_SIZE / 2 - 2, 3, highlightColor);
            
            // Draw the orb symbol
            Minecraft mc = Minecraft.getInstance();
            String symbol = "◆";
            int symbolWidth = mc.font.width(symbol);
            guiGraphics.drawString(mc.font, symbol, 
                    x + (ORB_SIZE - symbolWidth) / 2, 
                    y + (ORB_SIZE - 8) / 2, 
                    0xFFFFFFFF, false);
        } else {
            // Inactive orb - dim border only
            int borderColor = 0xFF444455;
            drawCircleBorder(guiGraphics, x + ORB_SIZE / 2, y + ORB_SIZE / 2, ORB_SIZE / 2 - 1, borderColor);
            
            // Draw empty symbol
            Minecraft mc = Minecraft.getInstance();
            String symbol = "◇";
            int symbolWidth = mc.font.width(symbol);
            guiGraphics.drawString(mc.font, symbol, 
                    x + (ORB_SIZE - symbolWidth) / 2, 
                    y + (ORB_SIZE - 8) / 2, 
                    0xFF555566, false);
        }
    }
    
    /**
     * Draw a filled circle using rectangle approximation
     */
    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        // Approximate circle with horizontal lines
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            guiGraphics.fill(centerX - dx, centerY + dy, centerX + dx, centerY + dy + 1, color);
        }
    }
    
    /**
     * Draw a circle border
     */
    private void drawCircleBorder(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = (double) i / segments * 2 * Math.PI;
            double angle2 = (double) (i + 1) / segments * 2 * Math.PI;
            
            int x1 = centerX + (int) (Math.cos(angle1) * radius);
            int y1 = centerY + (int) (Math.sin(angle1) * radius);
            int x2 = centerX + (int) (Math.cos(angle2) * radius);
            int y2 = centerY + (int) (Math.sin(angle2) * radius);
            
            // Draw small rectangle between points
            guiGraphics.fill(Math.min(x1, x2), Math.min(y1, y2), 
                    Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }
}
