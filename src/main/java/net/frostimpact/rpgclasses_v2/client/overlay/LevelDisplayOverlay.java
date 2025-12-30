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
 * Overlay that displays the player's level and class level in the top-left corner
 */
public class LevelDisplayOverlay implements LayeredDraw.Layer {
    private static final int MARGIN = 5;
    private static final int PANEL_WIDTH = 130; // Increased from 120 to fit longer class names
    private static final int LINE_HEIGHT = 12;
    private static final int XP_PER_CLASS_LEVEL = 100; // Base XP required per class level
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // Don't render if any screen is open
        if (mc.screen != null) return;
        
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        
        int panelX = MARGIN;
        int panelY = MARGIN;
        
        RenderSystem.enableBlend();
        
        // Calculate panel height based on content
        int contentLines = 3; // Level, Class Level, Class Name
        int panelHeight = contentLines * LINE_HEIGHT + 12;
        
        // Draw panel background with gradient
        guiGraphics.fillGradient(panelX, panelY, 
                panelX + PANEL_WIDTH, panelY + panelHeight, 
                0xCC000000, 0x99000000);
        
        // Draw panel border
        int borderColor = getClassBorderColor(rpgData.getCurrentClass());
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, borderColor);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, borderColor);
        guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, borderColor);
        guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + PANEL_WIDTH, panelY + panelHeight, borderColor);
        
        int textX = panelX + 5;
        int currentY = panelY + 5;
        
        // Class Name with icon
        String className = rpgData.getCurrentClass();
        String classIcon = getClassIcon(className);
        String classDisplay = classIcon + " " + formatClassName(className);
        int classColor = getClassColor(className);
        guiGraphics.drawString(mc.font, classDisplay, textX, currentY, classColor, false);
        currentY += LINE_HEIGHT;
        
        // Player Level (Minecraft XP level)
        int xpLevel = player.experienceLevel;
        String levelText = "§aLv. " + xpLevel;
        guiGraphics.drawString(mc.font, levelText, textX, currentY, 0xFF55FF55, false);
        currentY += LINE_HEIGHT;
        
        // Class Level with progress bar
        int classLevel = rpgData.getClassLevel();
        int classXP = rpgData.getClassExperience();
        int xpNeeded = classLevel * XP_PER_CLASS_LEVEL;
        float progress = (float) classXP / xpNeeded;
        
        String classLevelText = "§bClass Lv. " + classLevel;
        guiGraphics.drawString(mc.font, classLevelText, textX, currentY, 0xFF55FFFF, false);
        
        // XP progress bar
        int barX = panelX + 5;
        int barY = currentY + LINE_HEIGHT - 2;
        int barWidth = PANEL_WIDTH - 10;
        int barHeight = 3;
        
        // Background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        
        // Progress fill
        int fillWidth = (int) (barWidth * progress);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFF55AAFF);
        }
        
        RenderSystem.disableBlend();
    }
    
    private String formatClassName(String className) {
        if (className == null || className.isEmpty() || className.equals("NONE")) {
            return "No Class";
        }
        // Capitalize first letter
        return className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
    }
    
    private String getClassIcon(String classId) {
        if (classId == null) return "⭐";
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔";
            case "mage" -> "✨";
            case "rogue" -> "🗡";
            case "ranger" -> "🏹";
            case "tank" -> "🛡";
            case "priest" -> "❤";
            case "berserker" -> "💢";
            case "paladin" -> "✝";
            case "pyromancer" -> "🔥";
            case "frostmage" -> "❄";
            case "assassin" -> "☠";
            case "shadowdancer" -> "👤";
            case "hawkeye" -> "👁";
            case "marksman" -> "🎯";
            case "beastmaster" -> "🐺";
            case "guardian" -> "🏰";
            case "juggernaut" -> "💪";
            case "cleric" -> "💚";
            case "templar" -> "⚡";
            default -> "⭐";
        };
    }
    
    private int getClassColor(String classId) {
        if (classId == null) return 0xFFAAAAAA;
        return switch (classId.toLowerCase()) {
            case "warrior", "berserker" -> 0xFFFF4444;
            case "paladin" -> 0xFFFFDD44;
            case "mage", "pyromancer", "frostmage" -> 0xFFAA44FF;
            case "rogue", "assassin", "shadowdancer" -> 0xFF44FF44;
            case "ranger", "hawkeye", "marksman", "beastmaster" -> 0xFF88DD44;
            case "tank", "guardian", "juggernaut" -> 0xFF44AAFF;
            case "priest", "cleric", "templar" -> 0xFFFFAA44;
            default -> 0xFFAAAAAA;
        };
    }
    
    private int getClassBorderColor(String classId) {
        if (classId == null) return 0xFF666666;
        return switch (classId.toLowerCase()) {
            case "warrior", "berserker" -> 0xFFCC3333;
            case "paladin" -> 0xFFCCAA33;
            case "mage", "pyromancer", "frostmage" -> 0xFF8833CC;
            case "rogue", "assassin", "shadowdancer" -> 0xFF33CC33;
            case "ranger", "hawkeye", "marksman", "beastmaster" -> 0xFF66AA33;
            case "tank", "guardian", "juggernaut" -> 0xFF3388CC;
            case "priest", "cleric", "templar" -> 0xFFCC8833;
            default -> 0xFF666666;
        };
    }
}
