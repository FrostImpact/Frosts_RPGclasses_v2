package net.frostimpact.rpgclasses_v2.rpg.stats;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stats panel overlay in top-right corner (toggle with keybind)
 */
public class StatsDropdownOverlay implements LayeredDraw.Layer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsDropdownOverlay.class);
    private static final int MARGIN = 5;
    private static final int PANEL_WIDTH = 180;
    private static final int LINE_HEIGHT = 12;
    
    private static boolean isExpanded = false;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !isExpanded) return;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Position in top-right corner
        int panelX = screenWidth - PANEL_WIDTH - MARGIN;
        int panelY = MARGIN;
        
        RenderSystem.enableBlend();
        
        // Draw stats panel
        PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
        PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
        
        int contentHeight = 10 * LINE_HEIGHT + 10; // 9 stats + padding
        
        // Background
        guiGraphics.fill(panelX, panelY, 
                       panelX + PANEL_WIDTH, panelY + contentHeight, 0xDD000000);
        
        // Border with gradient
        guiGraphics.fill(panelX, panelY, 
                       panelX + PANEL_WIDTH, panelY + 2, 0xFFFFDD00);
        guiGraphics.fill(panelX, panelY, 
                       panelX + 2, panelY + contentHeight, 0xFFFFDD00);
        guiGraphics.fill(panelX + PANEL_WIDTH - 2, panelY, 
                       panelX + PANEL_WIDTH, panelY + contentHeight, 0xFFAA8800);
        guiGraphics.fill(panelX, panelY + contentHeight - 2, 
                       panelX + PANEL_WIDTH, panelY + contentHeight, 0xFFAA8800);
        
        // Draw stats with icons
        int iconSize = 8;
        int textStartX = panelX + 5 + iconSize + 3;
        int currentY = panelY + 5;
        
        // Health
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFFFF5555);
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int maxHealthBonus = stats.getIntStatValue(StatType.MAX_HEALTH);
        String healthText = String.format("Health: %.1f/%.1f (+%d)", health, maxHealth, maxHealthBonus);
        guiGraphics.drawString(mc.font, healthText, textStartX, currentY, 0xFFFF5555);
        currentY += LINE_HEIGHT;
        
        // Mana
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFF55FFFF);
        int mana = rpgData.getMana();
        int maxMana = rpgData.getMaxMana();
        int maxManaBonus = stats.getIntStatValue(StatType.MAX_MANA);
        String manaText = String.format("Mana: %d/%d (+%d)", mana, maxMana, maxManaBonus);
        guiGraphics.drawString(mc.font, manaText, textStartX, currentY, 0xFF55FFFF);
        currentY += LINE_HEIGHT;
        
        // Damage
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFFFFAA00);
        int damageBonus = stats.getIntStatValue(StatType.DAMAGE);
        String damageText = String.format("Damage: +%d", damageBonus);
        guiGraphics.drawString(mc.font, damageText, textStartX, currentY, 0xFFFFAA00);
        currentY += LINE_HEIGHT;
        
        // Defense
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFF00AAFF);
        int defenseBonus = stats.getIntStatValue(StatType.DEFENSE);
        String defenseText = String.format("Defense: +%d", defenseBonus);
        guiGraphics.drawString(mc.font, defenseText, textStartX, currentY, 0xFF00AAFF);
        currentY += LINE_HEIGHT;
        
        // Move Speed (percentage)
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFF55FF55);
        double moveSpeedBonus = stats.getPercentageStatValue(StatType.MOVE_SPEED);
        String moveSpeedText = String.format("Move Speed: +%.1f%%", moveSpeedBonus);
        guiGraphics.drawString(mc.font, moveSpeedText, textStartX, currentY, 0xFF55FF55);
        currentY += LINE_HEIGHT;
        
        // Attack Speed (percentage)
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFFFF55FF);
        double attackSpeedBonus = stats.getPercentageStatValue(StatType.ATTACK_SPEED);
        String attackSpeedText = String.format("Attack Speed: +%.1f%%", attackSpeedBonus);
        guiGraphics.drawString(mc.font, attackSpeedText, textStartX, currentY, 0xFFFF55FF);
        currentY += LINE_HEIGHT;
        
        // Cooldown Reduction
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFFAA55FF);
        int cooldownReduction = stats.getIntStatValue(StatType.COOLDOWN_REDUCTION);
        String cooldownText = String.format("Cooldown Reduction: +%d", cooldownReduction);
        guiGraphics.drawString(mc.font, cooldownText, textStartX, currentY, 0xFFAA55FF);
        currentY += LINE_HEIGHT;
        
        // Health Regen
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFFFF8888);
        int healthRegen = stats.getIntStatValue(StatType.HEALTH_REGEN);
        String healthRegenText = String.format("Health Regen: +%d", healthRegen);
        guiGraphics.drawString(mc.font, healthRegenText, textStartX, currentY, 0xFFFF8888);
        currentY += LINE_HEIGHT;
        
        // Mana Regen
        drawStatIcon(guiGraphics, panelX + 5, currentY, iconSize, 0xFF88FFFF);
        int manaRegen = stats.getIntStatValue(StatType.MANA_REGEN);
        String manaRegenText = String.format("Mana Regen: +%d", manaRegen);
        guiGraphics.drawString(mc.font, manaRegenText, textStartX, currentY, 0xFF88FFFF);
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Draw a simple colored icon for a stat
     */
    private void drawStatIcon(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        // Draw icon background
        guiGraphics.fill(x, y, x + size, y + size, color);
        // Draw white border
        guiGraphics.fill(x, y, x + size, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + size, 0xFFFFFFFF);
        guiGraphics.fill(x + size - 1, y, x + size, y + size, 0xFF888888);
        guiGraphics.fill(x, y + size - 1, x + size, y + size, 0xFF888888);
        // Draw center dot
        guiGraphics.fill(x + size / 2 - 1, y + size / 2 - 1, 
                       x + size / 2 + 1, y + size / 2 + 1, 0xFFFFFFFF);
    }
    
    /**
     * Toggle panel expansion - to be called from client input handler
     */
    public static void toggleDropdown() {
        isExpanded = !isExpanded;
        LOGGER.debug("Stats panel toggled. Now {}", isExpanded ? "expanded" : "collapsed");
    }
}
