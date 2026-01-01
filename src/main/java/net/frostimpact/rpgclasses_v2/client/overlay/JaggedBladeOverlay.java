package net.frostimpact.rpgclasses_v2.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.frostimpact.rpgclasses_v2.rpg.stats.StatType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Overlay that displays Ravager's Jagged Blade passive indicator
 * Shows that attack speed is being converted to BLEED duration
 */
public class JaggedBladeOverlay implements LayeredDraw.Layer {
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 120;
    private static final int INDICATOR_WIDTH = 120;
    private static final int INDICATOR_HEIGHT = 40;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        String currentClass = rpgData.getCurrentClass();
        
        // Only show for Ravager class
        if (currentClass == null || !currentClass.equalsIgnoreCase("ravager")) return;
        
        var stats = player.getData(ModAttachments.PLAYER_STATS);
        int attackSpeedBonus = stats.getIntStatValue(StatType.ATTACK_SPEED);
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        
        // Position in top-right area below seeker charges
        int x = screenWidth - MARGIN_RIGHT - INDICATOR_WIDTH;
        int y = MARGIN_TOP;
        
        RenderSystem.enableBlend();
        
        // Draw background panel
        int bgColor = 0xC0200000;
        guiGraphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, bgColor);
        
        // Draw border
        int borderColor = 0xFF800000;
        guiGraphics.fill(x, y, x + INDICATOR_WIDTH, y + 1, borderColor); // Top
        guiGraphics.fill(x, y + INDICATOR_HEIGHT - 1, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, borderColor); // Bottom
        guiGraphics.fill(x, y, x + 1, y + INDICATOR_HEIGHT, borderColor); // Left
        guiGraphics.fill(x + INDICATOR_WIDTH - 1, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, borderColor); // Right
        
        // Draw title
        String title = "§c⚔ Jagged Blade";
        guiGraphics.drawString(mc.font, title, x + 5, y + 5, 0xFFFFFF, true);
        
        // Draw attack speed conversion info
        String conversionText = "§7Attack Speed: §e" + attackSpeedBonus + "%";
        guiGraphics.drawString(mc.font, conversionText, x + 5, y + 17, 0xFFFFFF, false);
        
        // Calculate BLEED duration
        int bleedDuration = 60 + (attackSpeedBonus * 2);
        float bleedDurationSeconds = bleedDuration / 20.0f;
        String durationText = "§7BLEED: §c" + String.format("%.1f", bleedDurationSeconds) + "s";
        guiGraphics.drawString(mc.font, durationText, x + 5, y + 28, 0xFFFFFF, false);
        
        RenderSystem.disableBlend();
    }
}
