package net.frostimpact.rpgclasses_v2.rpg.stats;

import net.frostimpact.rpgclasses_v2.networking.ModMessages;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketAllocateStatPoint;
import net.frostimpact.rpgclasses_v2.networking.packet.PacketResetStats;
import net.frostimpact.rpgclasses_v2.rpg.ModAttachments;
import net.frostimpact.rpgclasses_v2.rpg.PlayerRPGData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screen for allocating stat points
 */
public class StatAllocationScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatAllocationScreen.class);
    private static final int PANEL_WIDTH = 300;
    private static final int LINE_HEIGHT = 25;
    
    private PlayerStats stats;
    private PlayerRPGData rpgData;
    
    public StatAllocationScreen() {
        super(Component.literal("Stat Allocation"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        stats = mc.player.getData(ModAttachments.PLAYER_STATS);
        rpgData = mc.player.getData(ModAttachments.PLAYER_RPG);
        
        int startY = 80;
        int buttonWidth = 30;
        int buttonHeight = 20;
        
        // Add buttons for each stat type
        StatType[] allocatableStats = {
            StatType.MAX_HEALTH,
            StatType.MAX_MANA,
            StatType.DAMAGE,
            StatType.DEFENSE,
            StatType.MOVE_SPEED,
            StatType.ATTACK_SPEED
        };
        
        for (int i = 0; i < allocatableStats.length; i++) {
            final StatType statType = allocatableStats[i];
            int buttonY = startY + (i * LINE_HEIGHT);
            int buttonX = (this.width + PANEL_WIDTH) / 2 - buttonWidth - 10;
            
            this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                button -> allocateStat(statType)
            ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
        }
        
        // Add reset button
        this.addRenderableWidget(Button.builder(
            Component.literal("Reset Stats"),
            button -> resetStats()
        ).bounds((this.width - 200) / 2, this.height - 65, 90, 20).build());
        
        // Add close button
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose()
        ).bounds((this.width - 200) / 2 + 100, this.height - 65, 90, 20).build());
    }
    
    private void allocateStat(StatType statType) {
        if (rpgData.getAvailableStatPoints() > 0) {
            // Send packet to server
            ModMessages.sendToServer(new PacketAllocateStatPoint(statType));
            LOGGER.debug("Allocating stat point to: {}", statType);
        }
    }
    
    private void resetStats() {
        // Send packet to server to reset all allocated stats
        ModMessages.sendToServer(new PacketResetStats());
        LOGGER.debug("Resetting all allocated stat points");
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        if (stats == null || rpgData == null) return;
        
        // Draw title
        String title = "Stat Allocation";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, 
            (this.width - titleWidth) / 2, 20, 0xFFFFFFFF);
        
        // Draw available points
        String pointsText = "Available Points: " + rpgData.getAvailableStatPoints();
        int pointsWidth = this.font.width(pointsText);
        guiGraphics.drawString(this.font, pointsText, 
            (this.width - pointsWidth) / 2, 50, 0xFF55FF55);
        
        // Draw panel background
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int startY = 80;
        int panelHeight = 6 * LINE_HEIGHT + 20;
        
        guiGraphics.fill(panelX, startY - 5, 
                       panelX + PANEL_WIDTH, startY + panelHeight, 0xDD000000);
        
        // Draw stats
        int textStartX = panelX + 10;
        int currentY = startY;
        
        Minecraft mc = Minecraft.getInstance();
        
        // Max Health
        int maxHealthBonus = stats.getIntStatValue(StatType.MAX_HEALTH);
        String healthText = String.format("❤ Max Health: +%d", maxHealthBonus);
        guiGraphics.drawString(mc.font, healthText, textStartX, currentY, 0xFFFF5555);
        currentY += LINE_HEIGHT;
        
        // Max Mana
        int maxManaBonus = stats.getIntStatValue(StatType.MAX_MANA);
        String manaText = String.format("⚡ Max Mana: +%d", maxManaBonus);
        guiGraphics.drawString(mc.font, manaText, textStartX, currentY, 0xFF55FFFF);
        currentY += LINE_HEIGHT;
        
        // Damage
        int damageBonus = stats.getIntStatValue(StatType.DAMAGE);
        String damageText = String.format("⚔ Damage: +%d", damageBonus);
        guiGraphics.drawString(mc.font, damageText, textStartX, currentY, 0xFFFFAA00);
        currentY += LINE_HEIGHT;
        
        // Defense
        int defenseBonus = stats.getIntStatValue(StatType.DEFENSE);
        String defenseText = String.format("🛡 Defense: +%d", defenseBonus);
        guiGraphics.drawString(mc.font, defenseText, textStartX, currentY, 0xFF00AAFF);
        currentY += LINE_HEIGHT;
        
        // Move Speed
        double moveSpeedBonus = stats.getPercentageStatValue(StatType.MOVE_SPEED);
        String moveSpeedText = String.format("👟 Move Speed: +%.1f%%", moveSpeedBonus);
        guiGraphics.drawString(mc.font, moveSpeedText, textStartX, currentY, 0xFF55FF55);
        currentY += LINE_HEIGHT;
        
        // Attack Speed
        double attackSpeedBonus = stats.getPercentageStatValue(StatType.ATTACK_SPEED);
        String attackSpeedText = String.format("🗡 Attack Speed: +%.1f%%", attackSpeedBonus);
        guiGraphics.drawString(mc.font, attackSpeedText, textStartX, currentY, 0xFFFF55FF);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
