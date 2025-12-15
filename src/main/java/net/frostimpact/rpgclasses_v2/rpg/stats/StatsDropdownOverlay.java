package net.frostimpact.rpgclasses_v2.rpg.stats;

import com.mojang.blaze3d.systems.RenderSystem;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Stats dropdown menu overlay in top-right corner
 */
public class StatsDropdownOverlay implements LayeredDraw.Layer {
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 15;
    private static final int MARGIN = 5;
    private static final int DROPDOWN_WIDTH = 180;
    private static final int LINE_HEIGHT = 12;
    
    private static boolean isExpanded = false;
    private static int lastMouseX = 0;
    private static int lastMouseY = 0;
    
    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Position in top-right corner
        int buttonX = screenWidth - BUTTON_WIDTH - MARGIN;
        int buttonY = MARGIN;
        
        // Update mouse position for click detection
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        
        // Check if mouse is over button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                           mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
        
        RenderSystem.enableBlend();
        
        // Draw button
        int buttonColor = isHovered ? 0xAA555555 : 0xAA333333;
        guiGraphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, buttonColor);
        guiGraphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + 1, 0xFFAAAAAA); // Top border
        guiGraphics.fill(buttonX, buttonY, buttonX + 1, buttonY + BUTTON_HEIGHT, 0xFFAAAAAA); // Left border
        guiGraphics.fill(buttonX + BUTTON_WIDTH - 1, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, 0xFF555555); // Right border
        guiGraphics.fill(buttonX, buttonY + BUTTON_HEIGHT - 1, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, 0xFF555555); // Bottom border
        
        // Draw button text
        String buttonText = isExpanded ? "Stats ▲" : "Stats ▼";
        int textX = buttonX + (BUTTON_WIDTH - mc.font.width(buttonText)) / 2;
        int textY = buttonY + (BUTTON_HEIGHT - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, buttonText, textX, textY, 0xFFFFFFFF);
        
        // Draw dropdown if expanded
        if (isExpanded) {
            PlayerStats stats = player.getData(ModAttachments.PLAYER_STATS);
            PlayerRPGData rpgData = player.getData(ModAttachments.PLAYER_RPG);
            
            int dropdownY = buttonY + BUTTON_HEIGHT + 2;
            int contentHeight = 10 * LINE_HEIGHT + 10; // 9 stats + padding
            
            // Background
            guiGraphics.fill(buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH), dropdownY, 
                           buttonX + BUTTON_WIDTH, dropdownY + contentHeight, 0xDD000000);
            
            // Border
            guiGraphics.fill(buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH), dropdownY, 
                           buttonX + BUTTON_WIDTH, dropdownY + 1, 0xFFAAAAAA); // Top
            guiGraphics.fill(buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH), dropdownY, 
                           buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH) + 1, dropdownY + contentHeight, 0xFFAAAAAA); // Left
            guiGraphics.fill(buttonX + BUTTON_WIDTH - 1, dropdownY, 
                           buttonX + BUTTON_WIDTH, dropdownY + contentHeight, 0xFF555555); // Right
            guiGraphics.fill(buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH), dropdownY + contentHeight - 1, 
                           buttonX + BUTTON_WIDTH, dropdownY + contentHeight, 0xFF555555); // Bottom
            
            // Draw stats
            int textStartX = buttonX - (DROPDOWN_WIDTH - BUTTON_WIDTH) + 5;
            int currentY = dropdownY + 5;
            
            // Health
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            double maxHealthBonus = stats.getStatValue(StatType.MAX_HEALTH);
            String healthText = String.format("Health: %.1f/%.1f (+%.0f%%)", health, maxHealth, maxHealthBonus);
            guiGraphics.drawString(mc.font, healthText, textStartX, currentY, 0xFFFF5555);
            currentY += LINE_HEIGHT;
            
            // Mana
            int mana = rpgData.getMana();
            int maxMana = rpgData.getMaxMana();
            double maxManaBonus = stats.getStatValue(StatType.MAX_MANA);
            String manaText = String.format("Mana: %d/%d (+%.0f%%)", mana, maxMana, maxManaBonus);
            guiGraphics.drawString(mc.font, manaText, textStartX, currentY, 0xFF55FFFF);
            currentY += LINE_HEIGHT;
            
            // Damage
            double damageBonus = stats.getStatValue(StatType.DAMAGE);
            String damageText = String.format("Damage: +%.1f%%", damageBonus);
            guiGraphics.drawString(mc.font, damageText, textStartX, currentY, 0xFFFFAA00);
            currentY += LINE_HEIGHT;
            
            // Defense
            double defenseBonus = stats.getStatValue(StatType.DEFENSE);
            String defenseText = String.format("Defense: +%.1f%%", defenseBonus);
            guiGraphics.drawString(mc.font, defenseText, textStartX, currentY, 0xFF00AAFF);
            currentY += LINE_HEIGHT;
            
            // Move Speed
            double moveSpeedBonus = stats.getStatValue(StatType.MOVE_SPEED);
            String moveSpeedText = String.format("Move Speed: +%.1f%%", moveSpeedBonus);
            guiGraphics.drawString(mc.font, moveSpeedText, textStartX, currentY, 0xFF55FF55);
            currentY += LINE_HEIGHT;
            
            // Attack Speed
            double attackSpeedBonus = stats.getStatValue(StatType.ATTACK_SPEED);
            String attackSpeedText = String.format("Attack Speed: +%.1f%%", attackSpeedBonus);
            guiGraphics.drawString(mc.font, attackSpeedText, textStartX, currentY, 0xFFFF55FF);
            currentY += LINE_HEIGHT;
            
            // Cooldown Reduction
            double cooldownReduction = stats.getStatValue(StatType.COOLDOWN_REDUCTION);
            String cooldownText = String.format("Cooldown Reduction: +%.1f%%", cooldownReduction);
            guiGraphics.drawString(mc.font, cooldownText, textStartX, currentY, 0xFFAA55FF);
            currentY += LINE_HEIGHT;
            
            // Health Regen
            double healthRegen = stats.getStatValue(StatType.HEALTH_REGEN);
            String healthRegenText = String.format("Health Regen: +%.1f%%", healthRegen);
            guiGraphics.drawString(mc.font, healthRegenText, textStartX, currentY, 0xFFFF8888);
            currentY += LINE_HEIGHT;
            
            // Mana Regen
            double manaRegen = stats.getStatValue(StatType.MANA_REGEN);
            String manaRegenText = String.format("Mana Regen: +%.1f%%", manaRegen);
            guiGraphics.drawString(mc.font, manaRegenText, textStartX, currentY, 0xFF88FFFF);
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Toggle dropdown expansion - to be called from client input handler
     */
    public static void toggleDropdown() {
        isExpanded = !isExpanded;
    }
    
    /**
     * Check if mouse is over the button - to be called from client input handler
     */
    public static boolean isMouseOverButton(int mouseX, int mouseY, int screenWidth) {
        int buttonX = screenWidth - BUTTON_WIDTH - MARGIN;
        int buttonY = MARGIN;
        return mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
               mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;
    }
}
